package io.mobilytix.reporting;

import com.aventstack.extentreports.Status;
import io.mobilytix.adb.AdbCommands;
import io.mobilytix.adb.ScreenRecorder;
import io.mobilytix.config.ConfigLoader;
import io.mobilytix.core.DriverManager;
import io.mobilytix.core.SessionContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Base64;


/**
 * TestNG listener that coordinates reporting for all test events.
 * <p>
 * Registered on BaseTest via @Listeners(MobilytixListener.class).
 * <p>
 * Responsibilities:
 * onTestStart   — create Extent test node, clear logcat, start recording
 * onTestSuccess — log pass to both reporters
 * onTestFailure — capture screenshot + logcat, attach to both reporters
 * onTestSkipped — log skip to both reporters
 * onFinish      — flush Extent report
 * <p>
 * Both ExtentReports and Allure are updated on every event so either
 * reporter can be used independently or together.
 */
public class MobilytixListener implements ITestListener {
    private static final Logger log = LogManager.getLogger(MobilytixListener.class);

    private static final String SCREENSHOT_LABEL = "Screenshot on failure";
    private static final String LOGCAT_LABEL = "Logcat on failure";
    private static final int LOGCAT_FAILURE_LINES = 200;
    private static final String LOG_PREFIX_START = "▶ TEST START  : {}";
    private static final String LOG_PREFIX_PASS = "✅ TEST PASS   : {}";
    private static final String LOG_PREFIX_FAIL = "❌ TEST FAIL   : {} — {}";
    private static final String LOG_PREFIX_SKIP = "⏭  TEST SKIP   : {}";

    @Override
    public void onTestStart(ITestResult result) {
        String testName = getTestName(result);
        String appKey = safeGetAppKey();

        log.info(LOG_PREFIX_START, testName);
        ExtentManager.createTest(testName, appKey);

        // Clear logcat so failure dumps only contain logs from this test
        safeAdbCall(() -> AdbCommands.getInstance().clearLogcat());
        // Start screen recording if enabled in config
        safeRecorderCall(() -> ScreenRecorder.getInstance().startRecording(testName));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = getTestName(result);
        log.info(LOG_PREFIX_PASS, testName);

        if (ExtentManager.getTest() != null) {
            ExtentManager.getTest().log(Status.PASS, "Test Passed");
        }
        // Attach screenshot on pass if configured
        if (ConfigLoader.getInstance().isScreenshotOnPass()) {
            attachScreenShotToExtent(SCREENSHOT_LABEL);
            AllureAttachments.attachScreenshot(SCREENSHOT_LABEL);
        }

        if (ConfigLoader.getInstance().isRunningOnSauceLabs()) {
            DriverManager.getInstance().getDriver().executeScript("sauce:job-result=passed");
        }

        stopRecordingAndAttach();
        ExtentManager.removeTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = getTestName(result);
        Throwable cause = result.getThrowable();
        log.error(LOG_PREFIX_FAIL, testName, cause != null ? cause.getMessage() : "unknown");
        // Screenshot — attach to both reporters
        attachScreenShotToExtent(SCREENSHOT_LABEL);
        AllureAttachments.attachScreenshot(SCREENSHOT_LABEL);

        // Logcat dump — attach to both reporters
        attachLogcatToExtent(LOGCAT_LABEL);

        // Mark test as failed in Extent
        if (ExtentManager.getTest() != null && cause != null) {
            ExtentManager.getTest().fail(cause);
        }
        if (ConfigLoader.getInstance().isRunningOnSauceLabs()) {
            DriverManager.getInstance().getDriver().executeScript("sauce:job-result=failed");
        }
        stopRecordingAndAttach();
        ExtentManager.removeTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = getTestName(result);
        log.warn(LOG_PREFIX_SKIP, testName);

        if (ExtentManager.getTest() != null) {
            ExtentManager.getTest().log(Status.SKIP, "Test skipped");
        }

        ExtentManager.removeTest();
    }

    @Override
    public void onFinish(ITestContext context) {
        log.info("Suite finished — flushing reports");
        ExtentManager.flush();
    }

    /**
     * Returns the fully qualified test name including the class name.
     * Format: ClassName.methodName
     */
    private String getTestName(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
    }

    /**
     * Returns the current app key from SessionContext.
     * Returns "unknown" if SessionContext is not initialised —
     * prevents listener from throwing during cleanup edge cases.
     */
    private String safeGetAppKey() {
        try {
            return SessionContext.getAppKey();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Takes a screenshot and attaches it to the current ExtentTest.
     * Base64 encodes the bytes for Extent's inline image format.
     */
    private void attachScreenShotToExtent(String label) {
        try {
            if (ExtentManager.getTest() != null) {
                return;
            }
            byte[] screenshot = AllureAttachments.captureScreenshot();
            if (screenshot.length > 0) {
                String base64 = Base64.getEncoder().encodeToString(screenshot);
                ExtentManager.getTest().addScreenCaptureFromBase64String(base64, label);
                log.debug("Screenshot attached to Extent: {}", label);
            }
        } catch (Exception e) {
            log.warn("Failed to attach screenshot to Extent: {}", e.getMessage());
        }
    }

    /**
     * Captures logcat output and attaches it to both reporters.
     */
    private void attachLogcatToExtent(String label) {
        try {
            String logcat = AdbCommands.getInstance().getLogcat(LOGCAT_FAILURE_LINES);
            if (ExtentManager.getTest() != null) {
                ExtentManager.getTest().log(Status.INFO, "<pre>" + logcat + "</pre>");
            }
            AllureAttachments.attachLogcat(label, logcat);
        } catch (Exception e) {
            log.warn("Failed to attach logcat: {}", e.getMessage());
        }
    }

    /**
     * Stops the screen recording and attaches the file path to both reporters.
     */
    private void stopRecordingAndAttach() {
        safeRecorderCall(() -> {
            ScreenRecorder recorder = ScreenRecorder.getInstance();
            recorder.stopRecording();
            String path = recorder.getLastRecordingFilePath();
            if (path != null) {
                AllureAttachments.attachRecording(path);
                if (ExtentManager.getTest() != null) {
                    ExtentManager.getTest().log(Status.INFO, "Recording" + path);
                }
            }
        });
    }

    /**
     * Wraps an ADB call in a try-catch so a device communication failure
     * never causes the listener itself to throw and swallow the real failure.
     */
    private void safeAdbCall(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("ADB call in listener failed: {}", e.getMessage());
        }
    }

    /**
     * Wraps a ScreenRecorder call safely — recording is optional and
     * should never mask a real test failure.
     */
    private void safeRecorderCall(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("ScreenRecorder call in listener failed: {}", e.getMessage());
        }
    }

    /**
     * Checks if the driver is active before attempting screenshot/logcat.
     */
    private boolean isDriverActive() {
        return DriverManager.getInstance().isDriverActive();
    }
}
