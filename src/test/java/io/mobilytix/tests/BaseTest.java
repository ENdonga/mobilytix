package io.mobilytix.tests;

import io.mobilytix.adb.AdbCommands;
import io.mobilytix.annotation.AppUnderTest;
import io.mobilytix.config.AppConfig;
import io.mobilytix.config.ConfigLoader;
import io.mobilytix.core.AppiumServerManager;
import io.mobilytix.core.DriverManager;
import io.mobilytix.core.SessionContext;
import io.mobilytix.exceptions.AppiumServerException;
import io.mobilytix.reporting.MobilytixListener;
import io.mobilytix.utils.EnvLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.SkipException;
import org.testng.annotations.*;

/**
 * Base class for all test classes in the framework.
 * <p>
 * Every test class must:
 * 1. Extend BaseTest
 * 2. Be annotated with @AppUnderTest("app_key")
 * <p>
 * Example:
 *
 * @AppUnderTest("app_a") public class LoginTest extends BaseTest {
 * @Test public void testValidLogin() { ... }
 * }
 * <p>
 * Lifecycle managed here:
 * @BeforeSuite — start Appium server (if auto_start=true)
 * @BeforeClass(dependsOnMethods = "setUp") — resolve annotation, load config, init driver
 * @AfterClass — quit driver, clear session
 * @AfterSuite — flush reports, stop Appium server
 * <p>
 * Never put @BeforeSuite or @AfterSuite in a test class they belong here and run exactly once for the entire suite.
 * Never add alwaysRun = true to @BeforeClass in test classes.
 * It overrides dependsOnMethods and causes tests to run without a driver.
 */
@Listeners(MobilytixListener.class)
public abstract class BaseTest {
    protected final Logger log = LogManager.getLogger(this.getClass());
    private static final String ERROR_MISSING_ANNOTATION = "%s must be annotated with @AppUnderTest. Example: @AppUnderTest(\"app_a\")";
    private static volatile boolean suiteAborted = false;
    private static volatile String suiteAbortReason = null;

    /**
     * Starts the Appium server before any test class initialises.
     * If auto_start=false in config.yaml this is a no-op.
     * If the server is already running on the configured port this is a no-op.
     */
    @BeforeSuite(alwaysRun = true)
    public void globalSetup() {
        EnvLoader.load();

        log.info("========================================");
        log.info("  Mobilytix Suite Starting");
        log.info("========================================");
        try {
            AppiumServerManager.getInstance().startIfRequired();
            assertDeviceAndServerReady();
        } catch (Exception e) {
            suiteAborted = true;
            suiteAbortReason = e.getMessage();
            log.error("Suite aborted during pre-flight: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Flushes reports and stops the Appium server after all tests complete.
     * alwaysRun=true ensures this runs even if tests failed.
     */
    @AfterSuite(alwaysRun = true)
    public void globalTearDown() {
        log.info("======================================");
        log.info("  Mobilytix Suite Completed");
        log.info("======================================");
        AppiumServerManager.getInstance().stop();
    }

    /**
     * Resolves the @AppUnderTest annotation, loads config, and initialises
     * the AndroidDriver for the current test class.
     * <p>
     * Fails immediately with a clear message if:
     * - @AppUnderTest annotation is missing on the test class
     * - The app key does not exist in config.yaml
     * - The APK file is not found at the configured path
     * - The Appium server cannot create a session
     */
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        if (suiteAborted) {
            throw new SkipException("Suite aborted during pre-flight checks. Reason: " + suiteAbortReason);
        }
        AppUnderTest annotation = getClass().getAnnotation(AppUnderTest.class);
        if (annotation == null) {
            throw new IllegalStateException(String.format(ERROR_MISSING_ANNOTATION, getClass().getSimpleName()));
        }
        String appKey = annotation.value().trim().toLowerCase();
        log.info("Setting up | class: {} | app: {}", getClass().getSimpleName(), appKey);

        AppConfig appConfig = ConfigLoader.getInstance().getAppConfig(appKey);
        SessionContext.set(appKey, appConfig);
        DriverManager.getInstance().initDriver(appKey, appConfig);
        log.info("Setup complete | app {} | auth required: {}", appConfig.getAppName(), appConfig.isRequiresAuth());
    }

    /**
     * Quits the driver and clears session state after all tests in the class run.
     * alwaysRun=true ensures teardown runs even if a test or setup failed.
     */
    @AfterClass(alwaysRun = true)
    public void tearDown() {
        log.info("Tearing down | class {}", getClass().getSimpleName());
        DriverManager.getInstance().quitDriver();
        SessionContext.clear();
        log.info("Tearing down complete | class {}", getClass().getSimpleName());
    }

    /**
     * Returns the app key for the current test session.
     * Useful for conditional logic in tests that run against multiple apps.
     */
    protected String getAppKey() {
        return SessionContext.getAppKey();
    }

    /**
     * Returns the AppConfig for the current test session.
     * Useful for reading app-specific values like package name in tests.
     */
    protected AppConfig getAppConfig() {
        return SessionContext.getAppConfig();
    }


    /**
     * Returns true if the current session has been authenticated.
     */
    protected boolean isAuthenticated() {
        return SessionContext.isAuthenticated();
    }

    private void assertDeviceAndServerReady() {
        log.info("Running pre-flight checks...");
        ConfigLoader config = ConfigLoader.getInstance();
        // Check Appium server ready for connections
        if (!AppiumServerManager.getInstance().isServerRunning()) {
            throw new AppiumServerException(
                    "Pre-flight failed: Appium server is not running on " + config.getAppiumHost() + ":" + config.getAppiumPort() + "\n" +
                            "Fix: set auto_start: true in config.yaml or start Appium manually.");
        }
        log.info("Pre-flight: Appium server — OK");
        // Check device/emulator is ready
        AdbCommands.getInstance().assertDeviceReady();
        log.info("Pre-flight: Device — OK");
        log.info("Pre-flight checks passed — starting suite");
    }
}
