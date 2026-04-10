package io.mobilytix.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves environment-dependent tool paths at runtime.
 * <p>
 * Resolution order for ADB:
 * 1. ANDROID_HOME environment variable + /platform-tools/adb
 * 2. Plain "adb" — assumes it is already on the system PATH (CI environments)
 */
public class EnvUtils {
    private static final Logger log = LogManager.getLogger(EnvUtils.class);

    private static final String ENV_ANDROID_HOME = "ANDROID_HOME";
    private static final String PROP_ANDROID_HOME = "ANDROID_HOME";
    private static final String PLATFORM_TOOLS_SUBPATH = "platform-tools";
    private static final String ADB_BINARY = "adb";

    // Cached result — resolved once, reused on subsequent calls
    private static String cachedAdbPath = null;

    private EnvUtils() {
    }

    /**
     * Resolves the ADB executable path using the following strategy in order:
     * <p>
     * 1. ANDROID_HOME environment variable  (set in ~/.zshrc on Mac)
     * 2. ANDROID_HOME system property       (set via IntelliJ run config)
     * 3. Mac default SDK location           (~/Library/Android/sdk)
     * 4. Linux default SDK location         (~/Android/Sdk)
     * 5. Plain "adb"                        (relies on system PATH — CI)
     *
     * @return absolute path to adb binary, or "adb" as final fallback
     */
    public static String resolveAdbPath() {
        // Return cached result if already resolved
        if (cachedAdbPath != null) {
            log.debug("ADB path (cached): {}", cachedAdbPath);
            return cachedAdbPath;
        }
        // Strategy 1 — environment variable (Mac/Linux terminal, CI)
        String fromEnv = System.getenv(ENV_ANDROID_HOME);
        if (isValidSdkHome(fromEnv)) {
            return buildAdbPath(fromEnv, "ANDROID_HOME env var");
        }

        // Strategy 2 — JVM system property (IntelliJ run config fallback)
        String fromProp = System.getProperty(PROP_ANDROID_HOME);
        if (isValidSdkHome(fromProp)) {
            return buildAdbPath(fromProp, "ANDROID_HOME system property");
        }

        // Strategy 3 — rely on system PATH (CI where adb is on PATH directly)
        log.warn("ANDROID_HOME not found in environment or system properties. Falling back to 'adb' on system PATH. If this fails: \n" +
                "  Mac/Linux — ensure ANDROID_HOME is set in ~/.zshrc  IntelliJ  — see SETUP.md section on IDE configuration");
        cachedAdbPath = ADB_BINARY;
        return cachedAdbPath;
    }

    /**
     * Returns true if the given SDK home path is non-null, non-blank, and actually contains an adb binary.
     */
    private static boolean isValidSdkHome(String sdkHome) {
        if (sdkHome == null || sdkHome.isBlank()) return false;
        Path adbPath = Paths.get(sdkHome, PLATFORM_TOOLS_SUBPATH, ADB_BINARY);
        return adbPath.toFile().exists();
    }

    /**
     * Builds the full adb path from the SDK home and logs which resolution strategy succeeded.
     */
    private static String buildAdbPath(String sdkHome, String source) {
        Path adbPath = Paths.get(sdkHome, PLATFORM_TOOLS_SUBPATH, ADB_BINARY);
        log.debug("ADB resolved via {} : {}", source, adbPath);
        cachedAdbPath = adbPath.toString();
        return cachedAdbPath;
    }
}
