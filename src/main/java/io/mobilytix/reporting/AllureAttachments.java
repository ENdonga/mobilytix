package io.mobilytix.reporting;

import io.mobilytix.core.DriverManager;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility methods for attaching content to Allure reports.
 * <p>
 * All attachment methods are safe to call even if the driver is not active —
 * they log a warning and return gracefully rather than throwing.
 * <p>
 * Usage in tests:
 * AllureAttachments.step("Enter username");
 * AllureAttachments.attachScreenshot("After login");
 * AllureAttachments.attachText("Response", responseBody);
 */
public class AllureAttachments {
    private static final Logger log = LogManager.getLogger(AllureAttachments.class);

    private static final String MIME_PNG = "image/png";
    private static final String MIME_TEXT = "text/plain";
    private static final String EXT_PNG = ".png";
    private static final String EXT_TXT = ".txt";
    private static final String EXT_LOG = ".log";

    private AllureAttachments() {
    }

    /**
     * Marks a logical test step in the Allure report timeline.
     * Use this in test methods to label what each block of actions does.
     * <p>
     * Example:
     * AllureAttachments.step("Enter valid credentials");
     * loginPage.enterUsername("user@example.com");
     * loginPage.enterPassword("password");
     *
     * @param stepDescription human-readable description of the step
     */
    @Step("{stepDescription}")
    public static void step(String stepDescription) {
        log.trace("Step: {}", stepDescription);
    }

    /**
     * Takes a screenshot and attaches it to the current Allure test.
     * Safe to call — logs a warning if driver is not active.
     *
     * @param name label for the attachment in the Allure report
     */
    public static void attachScreenshot(String name) {
        try {
            byte[] screenshot = captureScreenshot();
            Allure.addAttachment(name, MIME_PNG, new ByteArrayInputStream(screenshot), EXT_PNG);
            log.trace("Screenshot attached to Allure: {}", name);
        } catch (Exception e) {
            log.warn("Failed to attach screenshot '{}': {}", name, e.getMessage());
        }
    }

    /**
     * Captures a screenshot and returns the raw bytes.
     * Used by MobilytixListener to attach to both Allure and ExtentReports.
     *
     * @return screenshot bytes or empty array if capture fails
     */
    public static byte[] captureScreenshot() {
        try {
            return DriverManager.getInstance().getDriver().getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            log.warn("Screenshot capture failed: {}", e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Attaches a plain text string to the current Allure test.
     * Use for API responses, config values, or any string content.
     *
     * @param name    label for the attachment
     * @param content the text content to attach
     */
    public static void attachText(String name, String content) {
        try {
            Allure.addAttachment(name, MIME_TEXT, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), EXT_TXT);
            log.trace("Text attached to Allure: {}", name);
        } catch (Exception e) {
            log.warn("Failed to attach text '{}': {}", name, e.getMessage());
        }
    }

    /**
     * Attaches logcat output to the current Allure test.
     * Called automatically by MobilytixListener on test failure.
     *
     * @param name   label for the attachment
     * @param logcat logcat output string
     */
    public static void attachLogcat(String name, String logcat) {
        try {
            Allure.addAttachment(name, MIME_TEXT, new ByteArrayInputStream(logcat.getBytes(StandardCharsets.UTF_8)), EXT_LOG);
            log.trace("Logcat attached to Allure: {}", name);
        } catch (Exception e) {
            log.warn("Failed to attach logcat '{}': {}", name, e.getMessage());
        }
    }

    /**
     * Attaches a screen recording file path as a text note.
     * Allure does not natively display MP4 files — the path is attached
     * as text so the tester knows where to find the recording.
     *
     * @param recordingPath absolute local path to the MP4 file
     */
    public static void attachRecording(String recordingPath) {
        if (recordingPath != null) {
            attachText("Screen recording path", recordingPath);
        }
    }
}
