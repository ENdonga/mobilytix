package io.mobilytix.adb;

import io.mobilytix.config.ConfigLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Records the device screen during test execution using ADB screenrecord.
 * <p>
 * Recording is controlled by reporting.screen_recording.enabled in config.yaml.
 * When enabled, recording starts before each test and stops after — the file
 * is then pulled from the device to the local output path.
 * <p>
 * Limitations:
 * - ADB screenrecord has a maximum duration of 3 minutes per file
 * - Recording runs in a background thread and does not block test execution
 * - Only available on physical devices and emulators running API 19+
 * <p>
 * Usage:
 * ScreenRecorder recorder = ScreenRecorder.getInstance();
 * recorder.start("LoginTest_testValidLogin");
 * // ... test runs ...
 * String localPath = recorder.stop();
 */
public class ScreenRecorder {
    private static final Logger log = LogManager.getLogger(ScreenRecorder.class);
    private static ScreenRecorder instance;

    private final AdbCommands adb = AdbCommands.getInstance();
    private final ConfigLoader config = ConfigLoader.getInstance();

    private static final String CMD_SCREEN_RECORD = "screenrecord";
    private static final String CMD_PKILL = "pkill";
    private static final String CMD_RM = "rm";
    private static final String SIGINT = "-2";
    private static final String DEVICE_RECORDING_DIR = "/sdcard/";
    private static final String FILE_EXTENSION = ".mp4";
    private static final String TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss";
    private static final String SAFE_NAME_REGEX = "[^a-zA-Z0-9_]";
    private static final String SAFE_NAME_REPLACEMENT = "_";
    private static final int THREAD_JOIN_TIMEOUT_MS = 5000;
    private static final int FILE_WRITE_DELAY_MS = 1000;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN);

    // State — one recording at a time per instance
    private String currentDevicePath;
    private String currentLocalPath;
    private Thread recordingThread;
    private volatile boolean recording = false;

    private ScreenRecorder() {
    }

    public static ScreenRecorder getInstance() {
        if (instance == null) {
            instance = new ScreenRecorder();
        }
        return instance;
    }

    /**
     * Starts a screen recording for the given test name.
     * Does nothing if screen_recording.enabled is false in config.yaml.
     * <p>
     * Recording runs in a background thread and does not block test execution.
     *
     * @param testName used to name the output file — spaces replaced with underscores
     */
    public void startRecording(String testName) {
        if (!config.isScreenRecordingEnabled()) {
            log.debug("Screen recording disabled in config — skipping");
            return;
        }
        if (recording) {
            log.warn("Recording already in progress — stopping previous recording first");
            stopRecording();
        }

        String fileName = buildFileName(testName);
        currentDevicePath = DEVICE_RECORDING_DIR + fileName + FILE_EXTENSION;
        currentLocalPath = buildLocalPath(fileName);

        log.info("Starting screen recording: {}", currentDevicePath);

        recording = true;
        recordingThread = new Thread(() ->
                adb.shell(CMD_SCREEN_RECORD, currentDevicePath));
        recordingThread.setDaemon(true);
        recordingThread.start();

        log.debug("Recording thread started for: {}", testName);
    }

    /**
     * Stops the current screen recording and pulls the file to the local
     * output directory configured in config.yaml.
     * <p>
     * Does nothing if no recording is in progress or recording is disabled.
     */
    public void stopRecording() {
        if (!config.isScreenRecordingEnabled() || !recording) {
            log.debug("No active recording to stop");
            return;
        }

        log.info("Stopping screen recording...");
        recording = false;

        // SIGINT (-2) finalises the MP4 file cleanly — SIGKILL would corrupt it
        adb.shell(CMD_PKILL, SIGINT, CMD_SCREEN_RECORD);

        waitForRecordingThread();
        waitForFileWrite();
        ensureOutputDirExists();

        log.info("Pulling recording to: {}", currentLocalPath);
        adb.pullFile(currentDevicePath, currentLocalPath);
        adb.shell(CMD_RM, currentDevicePath);

        log.info("Screen recording saved to: {}", currentLocalPath);
    }

    /**
     * Returns the local path where the last recording was saved.
     * Returns null if no recording has been completed yet.
     * Always call stopRecording() before this.
     */
    public String getLastRecordingFilePath() {
        return currentLocalPath;
    }

    /**
     * Builds the full local output path for the pulled recording.
     */
    private String buildLocalPath(String fileName) {
        return Paths.get(config.getScreenRecordingOutputPath(), fileName + FILE_EXTENSION).toAbsolutePath().toString();
    }

    /**
     * Builds a timestamped file name for the recording.
     * Sanitises the test name to be filesystem safe.
     */
    private String buildFileName(String testName) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String safeName = testName.replaceAll(SAFE_NAME_REGEX, SAFE_NAME_REPLACEMENT);
        return safeName + SAFE_NAME_REPLACEMENT + timestamp;
    }

    /**
     * Creates the local output directory if it does not exist.
     */
    private void ensureOutputDirExists() {
        File outputDir = new File(config.getScreenRecordingOutputPath());
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs();
            if (created) {
                log.debug("Created screen recording output directory: {}", outputDir);
            }
        }
    }

    private void waitForRecordingThread() {
        if (recordingThread != null) {
            try {
                recordingThread.join(THREAD_JOIN_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for recording thread");
            }
        }
    }

    private void waitForFileWrite() {
        try {
            Thread.sleep(FILE_WRITE_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
