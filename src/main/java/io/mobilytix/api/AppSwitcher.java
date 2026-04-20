package io.mobilytix.api;

import io.mobilytix.adb.AdbCommands;
import io.mobilytix.adb.ApkManager;
import io.mobilytix.config.AppConfig;
import io.mobilytix.config.ConfigLoader;
import io.mobilytix.core.DriverManager;
import io.mobilytix.core.SessionContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

/**
 * High-level app lifecycle management.
 * <p>
 * Provides operations for resetting, reinstalling, backgrounding,
 * and foregrounding the app under test.
 * <p>
 * All operations are scoped to the current thread's session via SessionContext.
 * <p>
 * Usage:
 * AppSwitcher.getInstance().resetApp();
 * AppSwitcher.getInstance().sendToBackground(3);
 * AppSwitcher.getInstance().bringToForeground();
 */
public class AppSwitcher {
    private static final Logger log = LogManager.getLogger(AppSwitcher.class);
    private static AppSwitcher instance;

    private final AdbCommands adb = AdbCommands.getInstance();
    private final ConfigLoader config = ConfigLoader.getInstance();
    private final ApkManager apkManager = ApkManager.getInstance();

    private static final int DEFAULT_BACKGROUND_SECONDS = 3;

    private AppSwitcher() {
    }

    public static AppSwitcher getInstance() {
        if (instance == null) {
            instance = new AppSwitcher();
        }
        return instance;
    }

    /**
     * Clears all app data and relaunches the app.
     * Equivalent to a fresh installation without reinstalling the APK.
     * Resets login state, preferences, and cached files.
     * <p>
     * Auth state in SessionContext is also reset so the next
     * authentication call is not skipped.
     */
    public void resetApp() {
        AppConfig appConfig = SessionContext.getAppConfig();
        String packageName = appConfig.getPackageName();
        log.info("Resetting app: {}", appConfig.getAppName());
        adb.forceStop(packageName);
        adb.clearAppData(packageName);
        DriverManager.getInstance().getDriver().activateApp(packageName);

        // Reset auth state — app data was cleared so user is logged out
        SessionContext.resetAuthState();
        log.info("App reset complete: {}", appConfig.getAppName());
    }

    /**
     * Uninstalls the current APK and re-installs from the central apks/store.
     * Guarantees a completely clean installation.
     * <p>
     * Use this when you need to verify first-launch behaviour or
     * when the app update flow is part of the test.
     *
     * @param appKey the app key from config.yaml
     */
    public void reinstallAndLaunchApp(String appKey) {
        AppConfig appConfig = config.getAppConfig(appKey);
        log.info("Reinstalling and launching app: {}", appConfig.getAppName());
        apkManager.reinstall(appKey);
        DriverManager.getInstance().getDriver().activateApp(appConfig.getAppName());
        SessionContext.resetAuthState();
        log.info("Reinstall and launch complete: {}", appConfig.getAppName());
    }

    /**
     * Brings the current app to the foreground if it was backgrounded.
     * Does nothing if the app is already in the foreground.
     */
    public void bringToForeground() {
        String packageName = SessionContext.getAppConfig().getPackageName();
        log.info("Bringing app to foreground: {}", packageName);
        DriverManager.getInstance().getDriver().activateApp(packageName);
    }

    /**
     * Sends the current app to the background for a specified duration,
     * then automatically brings it back to the foreground.
     * <p>
     * Use this to test app behaviour after interruption —
     * e.g. session timeout, background refresh, push notification handling.
     *
     * @param seconds how long to keep the app in the background
     */
    public void sendToBackground(int seconds) {
        log.info("Sending app to background for {}s", seconds);
        DriverManager.getInstance().getDriver().runAppInBackground(Duration.ofSeconds(seconds));
        log.info("App returned to foreground after {}s", seconds);
    }

    public void sendToBackground() {
        sendToBackground(DEFAULT_BACKGROUND_SECONDS);
    }

    /**
     * Terminates the app process without clearing data.
     * The app can be relaunched via bringToForeground().
     */
    public void terminateApp() {
        String packageName = SessionContext.getAppConfig().getPackageName();
        log.info("Terminating app: {}", packageName);
        DriverManager.getInstance().getDriver().terminateApp(packageName);
    }

    /**
     * Grants all permissions declared in the manifest to the current app.
     * Useful for bypassing permission dialogs in tests that are not
     * testing the permission flow itself.
     *
     * @param permissions varargs list of Android permission strings
     */
    public void grantPermission(String... permissions) {
        String packageName = SessionContext.getAppConfig().getPackageName();
        for (String permission : permissions) {
            adb.grantPermission(packageName, permission);
        }
    }
}
