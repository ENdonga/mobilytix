package io.mobilytix.adb;

import io.mobilytix.config.AppConfig;
import io.mobilytix.config.ConfigLoader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import io.mobilytix.exceptions.ApkNotFoundException;
import io.mobilytix.utils.EnvUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles APK installation and uninstallation.
 * <p>
 * Resolves APK paths from the central apks/ store using ConfigLoader.
 * All installs are done with the -r flag (replace existing installation)
 * so the method is safe to call whether or not the app is already installed.
 */
public class ApkManager {
    private static final Logger log = LogManager.getLogger(ApkManager.class);
    private static ApkManager instance;

    private final AdbCommands adb = AdbCommands.getInstance();
    private final String adbPath = EnvUtils.resolveAdbPath();
    private final ConfigLoader config = ConfigLoader.getInstance();
    private final String udid = config.getDeviceConfig().getUdid();

    private static final String FLAG_SERIAL = "-s";
    private static final String FLAG_REPLACE = "-r";
    private static final String CMD_INSTALL = "install";
    public static final String CMD_UNINSTALL = "uninstall";

    private ApkManager() {
    }

    public static synchronized ApkManager getInstance() {
        if (instance == null) {
            instance = new ApkManager();
        }
        return instance;
    }

    /**
     * Installs the APK for the given app key from the central apks/ store.
     * Replaces the existing installation if already present.
     *
     * @param appKey the app key from config.yaml e.g. "app_a"
     */
    public void install(String appKey) {
        AppConfig appConfig = config.getAppConfig(appKey);
        Path apkPath = resolveApkPath(appConfig.getApkPath());
        installFromPath(apkPath.toString());
        log.info("Installation complete for: {}", appConfig.getAppName());
    }

    /**
     * Installs an APK from an absolute file path.
     * Use this when installing an APK that is not in the central store.
     *
     * @param absoluteApkPath absolute path to the APK file
     */
    public void installFromPath(String absoluteApkPath) {
        log.info("Installing APK from path: {}", absoluteApkPath);
        adb.exec(List.of(adbPath, FLAG_SERIAL, udid, CMD_INSTALL, FLAG_REPLACE, absoluteApkPath));
    }

    /**
     * Uninstalls an app by package name.
     * Does nothing if the package is not installed.
     *
     * @param packageName the package to uninstall e.g. "com.example.app"
     */
    public void uninstall(String packageName) {
        if (!adb.isPackageInstalled(packageName)) {
            log.info("Package '{}' not installed - skipping uninstall", packageName);
            return;
        }
        log.info("Uninstalling: {}", packageName);
        adb.exec(List.of(adbPath, FLAG_SERIAL, udid, CMD_UNINSTALL, packageName));
        log.info("Uninstall complete: {}", packageName);
    }

    /**
     * Uninstalls then reinstalls the APK for the given app key.
     * Guarantees a clean installation - no residual data from previous runs.
     *
     * @param appKey the app key from config.yaml
     */
    public void reinstall(String appKey) {
        AppConfig appConfig = config.getAppConfig(appKey);
        log.info("Reinstalling: {}", appConfig.getAppName());
        uninstall(appConfig.getAppName());
        install(appKey);
    }

    /**
     * Returns true if the app for the given key is currently installed.
     *
     * @param appKey the app key from config.yaml
     * @return true if installed
     */
    public boolean isInstalled(String appKey) {
        String packageName = config.getAppConfig(appKey).getPackageName();
        return adb.isPackageInstalled(packageName);
    }

    /**
     * Resolves an APK path relative to the configured base path.
     * Validates the file exists before returning.
     */
    private Path resolveApkPath(String apkRelativePath) {
        Path resolvedPath = Paths.get(config.getApkBasePath(), apkRelativePath).toAbsolutePath();
        if (!resolvedPath.toFile().exists()) {
            throw new ApkNotFoundException(resolvedPath.toString());
        }
        return resolvedPath;
    }
}
