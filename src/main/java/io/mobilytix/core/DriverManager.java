package io.mobilytix.core;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.mobilytix.adb.AdbCommands;
import io.mobilytix.config.*;
import io.mobilytix.exceptions.ApkNotFoundException;
import io.mobilytix.exceptions.ConfigException;
import io.mobilytix.exceptions.DriverInitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.SessionNotCreatedException;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Manages the AndroidDriver instance per thread.
 * <p>
 * Uses ThreadLocal so parallel test execution is safe — each thread
 * gets its own driver and they never share state.
 * <p>
 * Usage:
 * DriverManager.getInstance().initDriver("app_a"); // create
 * DriverManager.getInstance().getDriver(); // access
 * DriverManager.getInstance().quitDriver(); // destroy
 */
public class DriverManager {
    private static final Logger log = LogManager.getLogger(DriverManager.class);
    // One driver per thread — the key to parallel execution safety
    private static final ThreadLocal<AndroidDriver> driverThread = new ThreadLocal<>();
    private static DriverManager instance;
    private final ConfigLoader config = ConfigLoader.getInstance();

    private static final Duration IMPLICIT_WAIT = Duration.ZERO;
    private static final String PLATFORM_ANDROID = "Android";
    private static final AtomicInteger deviceIndex = new AtomicInteger(0);

    private DriverManager() {
    }

    /**
     * Returns the singleton instance of DriverManager.
     * The singleton manages the ThreadLocal — one ThreadLocal shared across
     * all threads, each thread writes its own value into it.
     */
    public static synchronized DriverManager getInstance() {
        if (instance == null) {
            instance = new DriverManager();
        }
        return instance;
    }

    /**
     * Initialises a new AndroidDriver for the given app key.
     * Reads app and device configs, resolves the APK path, builds UiAutomator2Options, and creates the driver session.
     * <p>
     * Called from BaseTest @BeforeClass.
     *
     * @param appKey the app key from @AppUnderTest annotation
     * @throws SessionNotCreatedException if Appium cannot create the session
     */
    public void initDriver(String appKey, AppConfig appConfig) {
        DeviceConfig deviceConfig = config.getDeviceConfig();
        Path apkAbsolutePath = resolveApkPath(appConfig.getApkPath());
        log.info("Initializing driver | app: {} | device: {} | mode: {}",
                appConfig.getAppName(),
                config.isRunningOnSauceLabs() ? "Sauce Labs cloud" : deviceConfig.getUdid(),
                config.getExecutionMode()
        );
        UiAutomator2Options options = buildOptions(appConfig, deviceConfig, apkAbsolutePath);

        try {
            AndroidDriver driver = new AndroidDriver(resolveServerUrl(), options);
            driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT);
            driverThread.set(driver);
            log.info("Driver initialized successfully. Session ID: {}", driver.getSessionId());
        } catch (Exception ex) {
            log.error("Driver initialization failed for app: '{}':{}", appKey, ex.getMessage());
            throw new DriverInitException(appKey, ex);
        }
    }

    /**
     * Returns the driver for the current thread.
     *
     * @throws IllegalStateException if initDriver has not been called on this thread — prevents confusing NullPointerExceptions
     */
    public AndroidDriver getDriver() {
        AndroidDriver driver = driverThread.get();
        if (driver == null) {
            throw new IllegalStateException("Android Driver is not initialized for thread: " + Thread.currentThread().getName() + ". Ensure initDriver() was called in @BeforeClass");
        }
        return driver;
    }


    /**
     * Quits the driver and removes it from the ThreadLocal.
     * Always called in BaseTest @AfterClass — even if the test failed.
     * Safe to call if no driver is present.
     */
    public void quitDriver() {
        AndroidDriver driver = driverThread.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("Driver quit successfully for thread: {}", Thread.currentThread().getName());
            } catch (Exception ex) {
                log.error("Driver quit failed: {}", ex.getMessage());
            } finally {
                driverThread.remove();
                log.debug("Driver removed successfully for ThreadLocal thread: {}", Thread.currentThread().getName());
            }
        }
    }

    /**
     * Returns true if a driver is active on the current thread.
     * Useful for conditional cleanup in listeners.
     */
    public boolean isDriverActive() {
        try {
            AndroidDriver driver = driverThread.get();
            return driver != null && driver.getSessionId() != null;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Resolves the APK path from config to an absolute path.
     * apkPath in config is relative to apk_base_path which is relative to the project root (current working directory).
     */
    private Path resolveApkPath(String apkPath) {
        Path resolvedPath = Paths.get(config.getApkBasePath(), apkPath).toAbsolutePath();
        if (config.isRunningOnSauceLabs()) {
            log.debug("Sauce Labs mode — skipping local APK file check");
            return resolvedPath;
        }
        log.info("Resolved path: {}", resolvedPath);
        if (!resolvedPath.toFile().exists()) {
            throw new ApkNotFoundException(resolvedPath.toString());
        }
        return resolvedPath;
    }

    /**
     * Builds the UiAutomator2Options capability set from config objects.
     */
    private UiAutomator2Options buildOptions(AppConfig appConfig, DeviceConfig deviceConfig, Path apkPath) {
        UiAutomator2Options options = new UiAutomator2Options()
                .setAutomationName(deviceConfig.getAutomationName())
                .setNewCommandTimeout(Duration.ofSeconds(deviceConfig.getNewCommandTimeout()))
                .setNoReset(deviceConfig.isNoReset())
                .setFullReset(deviceConfig.isFullReset());

        if (config.isRunningOnSauceLabs()) {
            applySauceLabsOptions(options, appConfig);
        } else {
            applyLocalOptions(options, appConfig, deviceConfig, apkPath);
        }
        return options;
    }

    private void applySauceLabsOptions(UiAutomator2Options options, AppConfig appConfig) {
        SauceLabsConfig sauceLabsConfig = config.getSauceLabsConfig();
        String username = config.getCredential(CredentialKeys.SAUCE_LABS_USERNAME, null);
        String accessKey = config.getCredential(CredentialKeys.SAUCE_LABS_ACCESS_KEY, null);
        String buildName = sauceLabsConfig.getBuild() + "-" + System.getenv().getOrDefault("GITHUB_RUN_NUMBER", "local");
        options.setApp("storage:filename=" + sauceLabsConfig.getAppStorageFilename())
                .setAppPackage(appConfig.getPackageName())
                .setAppActivity(appConfig.getActivity());
        options.setCapability("platformVersion", sauceLabsConfig.getPlatformVersion());
        options.setCapability("deviceName", sauceLabsConfig.getDeviceName());
        options.setCapability("sauce:options", Map.of(
                "username", username,
                "accessKey", accessKey,
                "build", buildName,
                "name", appConfig.getAppName(),
                "region", sauceLabsConfig.getRegion(),
                "deviceOrientation", "PORTRAIT"
        ));
        log.info("Sauce Labs options | device: {} | build: {}", sauceLabsConfig.getDeviceName(), buildName);
    }

    private void applyLocalOptions(UiAutomator2Options options, AppConfig appConfig, DeviceConfig deviceConfig, Path apkPath) {
        options.setUdid(resolvedUdid(deviceConfig))
                .setPlatformName(PLATFORM_ANDROID)
                .setPlatformVersion(deviceConfig.getPlatformVersion())
                .setApp(apkPath.toString())
                .setAppPackage(appConfig.getPackageName())
                .setAppActivity(appConfig.getActivity());
    }

    private String resolvedUdid(DeviceConfig deviceConfig) {
        // check if parallel devices are configured
        List<String> parallelDevices = config.getParallelDeviceUdids();
        if (parallelDevices.isEmpty()) {
            // Sequential run — use the single configured device
            return deviceConfig.getUdid();
        }
        // Only apply parallel assignment if running on a worker thread
        // Sequential runs use the main thread — parallel runs use pool threads
        boolean isParallelThread = !Thread.currentThread().getName().equals("main");

        if (!isParallelThread) {
            log.debug("Sequential run detected — using default device: {}", deviceConfig.getUdid());
            return deviceConfig.getUdid();
        }

        // Filter to only connected devices
        List<String> connectedDevices = AdbCommands.listConnectedDevices();
        List<String> availableDevices = parallelDevices.stream().filter(connectedDevices::contains).collect(Collectors.toList());
        if (availableDevices.isEmpty()) {
            log.warn("No Parallel devices connected - falling back to the default device: {}", deviceConfig.getUdid());
            return deviceConfig.getUdid();
        }
        // Parallel run — assign devices round-robin across threads
        int index = deviceIndex.getAndIncrement() % parallelDevices.size();
        String assignedUdid = parallelDevices.get(index);
        log.info("Parallel device assignment | thread: {} | udid: {}", Thread.currentThread(), assignedUdid);
        return assignedUdid;
    }

    /**
     * Returns the Appium server URL based on execution mode.
     * local → localhost Appium server managed by AppiumServerManager
     * sauce_labs → Sauce Labs cloud endpoint with embedded credentials
     */
    private URL resolveServerUrl() {
        if (config.isRunningOnSauceLabs()) {
            return buildSauceLabsUrl();
        }
        return AppiumServerManager.getInstance().getServiceUrl();
    }

    private URL buildSauceLabsUrl() {
        String username = config.getCredential(CredentialKeys.SAUCE_LABS_USERNAME, null);
        String accessKey = config.getCredential(CredentialKeys.SAUCE_LABS_ACCESS_KEY, null);
        String region = config.getSauceLabsConfig().getRegion();
        try {
            return new URL(String.format("https://%s:%s@ondemand.%s.saucelabs.com/wd/hub", username, accessKey, region));
//            return new URL(String.format("https://ondemand.%s.saucelabs.com/wd/hub", region));
        } catch (MalformedURLException e) {
            throw new ConfigException("Invalid Sauce Labs URL — check region: " + region, e);
        }
    }
}
