package io.mobilytix.core;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.mobilytix.config.AppConfig;
import io.mobilytix.config.ConfigLoader;
import io.mobilytix.config.DeviceConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.SessionNotCreatedException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

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
    public void initDriver(String appKey) {
        AppConfig appConfig = config.getAppConfig(appKey);
        DeviceConfig deviceConfig = config.getDeviceConfig();

        Path apkAbsolutePath = resolveApkPath(appConfig.getApkPath());
        log.info("Initializing driver | app: {} | device: {} | apk: {}", appConfig.getAppName(), deviceConfig.getUdid(), apkAbsolutePath);
        UiAutomator2Options options = buildOptions(appConfig, deviceConfig, apkAbsolutePath);

        try {
            AndroidDriver driver = new AndroidDriver(AppiumServerManager.getInstance().getServiceUrl(), options);
            // Explicit waits only — implicit wait interferes with expected condition checks like waitForInvisible
            driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT);
            driverThread.set(driver);
            log.info("Driver initialized successfully. Session ID: {}", driver.getSessionId());
        } catch (SessionNotCreatedException ex) {
            log.error("Driver initialization failed for app: '{}':{}", appKey, ex.getMessage());
            throw ex;
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
        log.info("Resolved path: {}", resolvedPath);
        if (!resolvedPath.toFile().exists()) {
            throw new RuntimeException("Apk file not found at: " + resolvedPath + ". Check APK path in config.yml and ensure APK is placed in the apks/ folder");
        }
        return resolvedPath;
    }

    /**
     * Builds the UiAutomator2Options capability set from config objects.
     */
    private UiAutomator2Options buildOptions(AppConfig appConfig, DeviceConfig deviceConfig, Path apkPath) {
        return new UiAutomator2Options()
                .setUdid(deviceConfig.getUdid())
                .setPlatformName(PLATFORM_ANDROID)
                .setPlatformVersion(deviceConfig.getPlatformVersion())
                .setApp(apkPath.toString())
                .setAppPackage(appConfig.getAppName())
                .setAppActivity(appConfig.getActivity())
                .setAutomationName(deviceConfig.getAutomationName())
                .setNewCommandTimeout(Duration.ofSeconds(deviceConfig.getNewCommandTimeout()))
                .setNoReset(deviceConfig.isNoReset())
                .setFullReset(deviceConfig.isFullReset());
    }
}
