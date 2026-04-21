package io.mobilytix.adb;

import io.mobilytix.config.ConfigLoader;
import io.mobilytix.exceptions.AdbCommandException;
import io.mobilytix.exceptions.DeviceNotReadyException;
import io.mobilytix.utils.EnvUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapper around the ADB command-line tool.
 * <p>
 * All commands are scoped to the device udid configured in config.yaml.
 * This prevents accidental execution on the wrong device when multiple devices are connected.
 * <p>
 * Usage:
 * AdbCommands adb = AdbCommands.getInstance();
 * adb.shell("input", "keyevent", "82");
 * adb.grantPermission("com.example.app", "android.permission.CAMERA");
 */
public class AdbCommands {
    private static final Logger log = LogManager.getLogger(AdbCommands.class);
    private static AdbCommands instance;

    private final String adbPath;
    private final String udid;

    // ADB Commands
    private static final String CMD_SHELL = "shell";
    private static final String CMD_PULL = "pull";
    private static final String CMD_PUSH = "push";
    private static final String CMD_LOGCAT = "logcat";
    private static final String CMD_DEVICES = "devices";
    private static final String CMD_GET_STATE = "get-state";
    // ADB flags
    private static final String FLAG_SERIAL = "-s";
    private static final String FLAG_DUMP = "-d";
    private static final String FLAG_TAIL = "-t";
    private static final String FLAG_CLEAR = "-c";
    private static final String FLAG_LOGCAT_SILENT = "-s";
    // ADB Shell sub-commands
    private static final String SHELL_PM = "pm";
    private static final String SHELL_AM = "am";
    private static final String SHELL_INPUT = "input";
    private static final String SHELL_GET_PROP = "getprop";
    // Package Manager sub-commands
    private static final String PM_GRANT = "grant";
    private static final String PM_REVOKE = "revoke";
    private static final String PM_CLEAR = "clear";
    private static final String PM_LIST = "list";
    private static final String PM_PACKAGES = "packages";
    private static final String PM_PACKAGE_PREFIX = "package:";
    // Activity Manager Sub commands
    private static final String AM_FORCE_STOP = "force-stop";
    // Input sub-commands
    private static final String INPUT_KEYEVENT = "keyevent";
    // Getprop keys
    private static final String PROP_ANDROID_VERSION = "ro.build.version.release";
    private static final String PROP_DEVICE_MODEL = "ro.product.model";
    // Key codes
    private static final String KEY_HOME = "3";
    private static final String KEY_BACK = "4";
    // Device filter
    private static final String DEVICE_FILTER = "\tdevice";
    private static final String DEVICE_SPLIT = "\t";
    private static final String DEVICE_STATE_ONLINE = "device";
    private static final String LOGCAT_SEPARATOR_PREFIX = "---------";

    private AdbCommands() {
        this.udid = ConfigLoader.getInstance().getDeviceConfig().getUdid();
        adbPath = EnvUtils.resolveAdbPath();
        log.debug("AdbCommands initialised | device: {} | adb: {}", udid, adbPath);
    }

    /**
     * Returns the singleton instance.
     */
    public static synchronized AdbCommands getInstance() {
        if (instance == null) {
            instance = new AdbCommands();
        }
        return instance;
    }

    /**
     * Returns true if the configured device is connected and ready.
     * Uses `adb get-state` which returns immediately — no hanging.
     */
    public boolean isDeviceReady() {
        try {
            String output = exec(List.of(adbPath, FLAG_SERIAL, udid, CMD_GET_STATE)).trim();
            boolean ready = output.equals(DEVICE_STATE_ONLINE);
            log.debug("Device '{}' state: '{}' — ready: {}", udid, output, ready);
            return ready;
        } catch (Exception e) {
            log.debug("Device '{}' not reachable: {}", udid, e.getMessage());
            return false;
        }
    }

    /**
     * Asserts the device is ready before proceeding.
     * Call this at the start of any operation that requires the device.
     * Throws a clear RuntimeException immediately instead of hanging.
     *
     * @throws RuntimeException if the device is not found or not ready
     */
    public void assertDeviceReady() {
        if (!isDeviceReady()) {
            throw new DeviceNotReadyException(udid);
        }
    }

    /**
     * Runs an adb shell command on the configured device.
     * <p>
     * Example:
     * shell("input", "keyevent", "82")
     * → adb -s emulator-5554 shell input keyevent 82
     *
     * @param args the shell command and its arguments
     * @return the combined stdout and stderr output as a string
     */
    public String shell(String... args) {
        List<String> cmd = new ArrayList<>(List.of(adbPath, FLAG_SERIAL, udid, CMD_SHELL));
        cmd.addAll(List.of(args));
        return exec(cmd);
    }

    /**
     * Grants a runtime permission to an app.
     * Equivalent to manually approving a permission dialog.
     *
     * @param packageName the app package e.g. "com.example.app"
     * @param permission  the Android permission e.g. "android.permission.CAMERA"
     */
    public void grantPermission(String packageName, String permission) {
        log.info("Granting permission '{}' to '{}'", permission, packageName);
        shell(SHELL_PM, PM_GRANT, packageName, permission);
    }

    /**
     * Revokes a runtime permission from an app.
     *
     * @param packageName the app package
     * @param permission  the Android permission to revoke
     */
    public void revokePermission(String packageName, String permission) {
        log.info("Revoking permission '{}' to '{}'", permission, packageName);
        shell(SHELL_PM, PM_REVOKE, packageName, permission);
    }

    /**
     * Clears all data for an app - equivalent to a fresh install without
     * reinstalling. Resets login state, preferences, cached files.
     *
     * @param packageName the app package to clear
     */
    public void clearAppData(String packageName) {
        log.info("Clearing app data for '{}'", packageName);
        shell(SHELL_PM, PM_CLEAR, packageName);
    }

    /**
     * Returns true if a package is installed on the device.
     *
     * @param packageName the package to check
     * @return true if installed
     */
    public boolean isPackageInstalled(String packageName) {
        String result = shell(SHELL_PM, PM_LIST, PM_PACKAGES, packageName);
        boolean installed = result.contains(PM_PACKAGE_PREFIX + packageName);
        log.debug("Package '{}' installed: {}", packageName, installed);
        return installed;
    }

    /**
     * Force stops an app process without clearing its data.
     *
     * @param packageName the app package to stop
     */
    public void forceStop(String packageName) {
        log.info("Forcing '{}' to stop", packageName);
        shell(SHELL_AM, AM_FORCE_STOP, packageName);
    }

    /**
     * Sends a key event to the device.
     * Common key codes:
     * 3  = HOME
     * 4  = BACK
     * 82 = MENU
     * 26 = POWER
     *
     * @param keyCode the Android KeyEvent code as a string
     */
    public void pressKey(String keyCode) {
        log.debug("Pressing key: {}", keyCode);
        shell(SHELL_INPUT, INPUT_KEYEVENT, keyCode);
    }

    /**
     * Sends the HOME key - navigates to the Android home screen.
     */
    public void pressHome() {
        pressKey(KEY_HOME);
    }

    /**
     * Sends the BACK key - navigates back one screen.
     */
    public void pressBack() {
        pressKey(KEY_BACK);
    }

    /**
     * Returns the last N lines of logcat filtered by tag.
     *
     * @param tag   the log tag to filter by e.g. "SmsRetriever", "MyApp"
     * @param lines number of lines to retrieve
     * @return logcat output as a string
     */
    public String getLogcat(String tag, int lines) {
        log.debug("Getting {} lines of logcat for tag: {}", lines, tag);
        List<String> cmd = new ArrayList<>(List.of(adbPath, FLAG_SERIAL, udid, CMD_LOGCAT, FLAG_DUMP, FLAG_TAIL, String.valueOf(lines), FLAG_LOGCAT_SILENT, tag));
        String output = filterLogcatNoise(exec(cmd));
        log.debug("[LOGCAT output]\n{}\n[END LOGCAT]", output);
        return output;
    }

    /**
     * Returns the last N lines of logcat with no tag filter.
     *
     * @param lines number of lines to retrieve
     * @return logcat output as a string
     */
    public String getLogcat(int lines) {
        log.debug("Getting {} lines of logcat (no tag filter)", lines);
        List<String> cmd = new ArrayList<>(List.of(adbPath, FLAG_SERIAL, udid, CMD_LOGCAT, FLAG_DUMP, FLAG_TAIL, String.valueOf(lines)));
        String output = filterLogcatNoise(exec(cmd));
        log.debug("[LOGCAT output]\n{}\n[END LOGCAT]", output);
        return output;
    }

    /**
     * Clears the logcat buffer on the device.
     * Call this before a test that needs to read fresh logcat output
     * e.g. before triggering an OTP SMS.
     */
    public void clearLogcat() {
        log.debug("Clearing logcat buffer");
        exec(List.of(adbPath, FLAG_SERIAL, udid, CMD_LOGCAT, FLAG_CLEAR));
    }

    /**
     * Pulls a file from the device to a local path.
     *
     * @param devicePath path on the device e.g. "/sdcard/recording.mp4"
     * @param localPath  destination path on your machine
     */
    public void pullFile(String devicePath, String localPath) {
        log.info("Pulling file: {} → {}", devicePath, localPath);
        exec(List.of(adbPath, FLAG_SERIAL, udid, CMD_PULL, devicePath, localPath));
    }

    /**
     * Pushes a local file to a path on the device.
     *
     * @param localPath  source path on your machine
     * @param devicePath destination path on the device
     */
    public void pushFile(String localPath, String devicePath) {
        log.info("Pushing file: {} → {}", localPath, devicePath);
        exec(List.of(adbPath, FLAG_SERIAL, udid, CMD_PUSH, localPath, devicePath));
    }

    /**
     * Returns the Android version string of the connected device.
     */
    public String getAndroidVersion() {
        return shell(SHELL_GET_PROP, PROP_ANDROID_VERSION).trim();
    }

    /**
     * Returns the device model name.
     */
    public String getDeviceModel() {
        return shell(SHELL_GET_PROP, PROP_DEVICE_MODEL).trim();
    }

    /**
     * Returns a list of serial numbers of all connected devices and emulators.
     * This is a static method - does not require a specific device.
     */
    public static List<String> listConnectedDevices() {
        String adb = EnvUtils.resolveAdbPath();
        try {
            Process p = new ProcessBuilder(adb, CMD_DEVICES).redirectErrorStream(true).start();
            return new BufferedReader(new InputStreamReader(p.getInputStream()))
                    .lines()
                    .filter(line -> line.contains(DEVICE_FILTER))
                    .map(line -> line.split(DEVICE_SPLIT)[0].trim())
                    .toList();
        } catch (Exception e) {
            throw new AdbCommandException("Failed to list connected devices", e);
        }
    }

    /**
     * Executes a command and returns its output.
     * Combines stdout and stderr into a single string.
     *
     * @param cmd the full command as a list of strings
     * @return the command output
     * @throws RuntimeException if the process cannot be started
     */
    public String exec(List<String> cmd) {
        log.trace("ADB exec: {}", String.join(" ", cmd));
        try {
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines()
                    .collect(Collectors.joining("\n"));
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("ADB command exited with code {}: {}", exitCode, output);
            } else if (!output.contains("\n")) {
                // Single line output — safe to log inline
                log.trace("ADB output: {}", output);
            }
            // Multi-line output (logcat) is logged by the caller with [LOGCAT] label
            return output;
        } catch (Exception e) {
            throw new AdbCommandException(String.join(" ", cmd), e);
        }
    }

    /**
     * Removes logcat buffer separator lines from output.
     * Android emits several variants:
     * "--------- beginning of main"
     * "--------- beginning of system"
     * "--------- beginning of crash"
     * "--------- end of main"
     * "--------- end of system"
     * "--------- switch to main"
     * "--------- switch to system"
     */
    private String filterLogcatNoise(String output) {
        return output.lines().filter(line -> !line.startsWith(LOGCAT_SEPARATOR_PREFIX)).collect(Collectors.joining("\n"));
    }
}
