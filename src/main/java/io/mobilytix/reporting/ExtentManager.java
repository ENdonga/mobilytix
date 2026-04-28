package io.mobilytix.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.mobilytix.config.ConfigLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Manages the ExtentReports singleton and per-thread test nodes.
 * <p>
 * ExtentReports is not thread-safe for test creation — synchronization
 * is applied on createTest() to prevent race conditions in parallel runs.
 * <p>
 * The report is written to the path configured in:
 * reporting.extent.output_path in config.yaml
 * <p>
 * Usage:
 * ExtentManager.createTest("testLoginSuccess", "app_a");
 * ExtentManager.getTest().info("Step completed");
 * ExtentManager.flush(); // called once at @AfterSuite
 */
public class ExtentManager {
    private static final Logger log = LogManager.getLogger(ExtentManager.class);
    private static final ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();

    private static final String REPORT_DOCUMENT_TITLE = "Mobilytix Test Report";
    private static final String REPORT_NAME = "Android Automation Results";
    private static final String SYSTEM_INFO_FRAMEWORK = "Framework";
    private static final String SYSTEM_INFO_OS = "OS";
    private static final String SYSTEM_INFO_JAVA = "Java";
    private static final String SYSTEM_INFO_APPIUM = "Appium";
    private static final String FRAMEWORK_NAME = "Mobilytix";
    private static final String APPIUM_VERSION = "2.x";
    private static final String PROP_OS_NAME = "os.name";
    private static final String PROP_JAVA_VERSION = "java.version";

    private static ExtentReports extent;

    private ExtentManager() {
    }

    /**
     * Returns the singleton ExtentReports instance.
     * Creates it on first call — subsequent calls return the cached instance.
     * Thread-safe via synchronized.
     */
    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            extent = createExtentReports();
        }
        return extent;
    }

    /**
     * Creates a new test node in the report for the current thread.
     * Assigns the app key as a category so tests are grouped by app.
     * <p>
     * Called from MobilytixListener.onTestStart().
     *
     * @param testName the test method name
     * @param appKey   the app key from @AppUnderTest — used as category
     */
    public static synchronized void createTest(String testName, String appKey) {
        ExtentTest test = getInstance().createTest(testName).assignCategory(appKey);
        testThread.set(test);
        log.trace("ExtentTest created: {} [{}]", testName, appKey);
    }

    /**
     * Returns the ExtentTest for the current thread.
     * Returns null if createTest() has not been called on this thread.
     */
    public static ExtentTest getTest() {
        return testThread.get();
    }

    /**
     * Removes the test node from the ThreadLocal.
     * Called after each test to prevent memory leaks in long parallel runs.
     */
    public static void removeTest() {
        testThread.remove();
    }

    /**
     * Writes all pending test results to the report file.
     * Must be called once at @AfterSuite — results are lost if not flushed.
     */
    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
            log.info("ExtentReports flushed to disk");
        }
    }

    private static ExtentReports createExtentReports() {
        ConfigLoader config = ConfigLoader.getInstance();
        String outputPath = config.getExtentOutputPath();
        String theme = config.getExtentTheme();

        ensureOutputDirectoryPathExists(outputPath);

        ExtentSparkReporter spark = new ExtentSparkReporter(outputPath);
        spark.config().setTheme(parseTheme(theme));
        spark.config().setDocumentTitle(REPORT_DOCUMENT_TITLE);
        spark.config().setReportName(REPORT_NAME);
        spark.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

        ExtentReports reports = new ExtentReports();
        reports.attachReporter(spark);
        reports.setSystemInfo(SYSTEM_INFO_FRAMEWORK, FRAMEWORK_NAME);
        reports.setSystemInfo(SYSTEM_INFO_OS, System.getProperty(PROP_OS_NAME));
        reports.setSystemInfo(SYSTEM_INFO_JAVA, System.getProperty(PROP_JAVA_VERSION));
        reports.setSystemInfo(SYSTEM_INFO_APPIUM, System.getProperty(APPIUM_VERSION));

        log.info("ExtentReports initialised at: {}", outputPath);
        return reports;
    }

    private static Theme parseTheme(String themeValue) {
        try {
            return Theme.valueOf(themeValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown theme '{}' — defaulting to DARK", themeValue);
            return Theme.DARK;
        }
    }

    private static void ensureOutputDirectoryPathExists(String outputPath) {
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                log.debug("Created report output directory: {}", parentDir);
            }
        }
    }
}
