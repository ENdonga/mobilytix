package io.mobilytix.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * Loads a .env file from the project root into system properties.
 * Called once at framework startup before ConfigLoader reads any values.
 * <p>
 * .env is in .gitignore - use it for local machine-specific overrides
 * like device udid, credentials, or API tokens.
 * <p>
 * Format:
 * KEY=value
 * DEVICE_UDID=emulator-5558
 * OTP_API_TOKEN=abc123
 */
public class EnvLoader {
    private static final Logger log = LogManager.getLogger(EnvLoader.class);
    private static final String ENV_FILE = ".env";
    private static boolean loaded = false;

    private EnvLoader() {
    }

    /**
     * Loads .env file values into system properties.
     * Safe to call multiple times - only loads once.
     * Skips silently if .env file does not exist.
     */
    public static synchronized void load() {
        if (loaded) {
            return;
        }
        File envFile = new File(ENV_FILE);
        if (!envFile.exists()) {
            log.debug(".env file not found at project root - skipping");
            loaded = true;
            return;
        }
        try (FileInputStream fis = new FileInputStream(envFile)) {
            Properties props = new Properties();
            props.load(fis);
            props.forEach((key, value) -> {
                String k = key.toString().trim();
                String v = value.toString().trim();
                // Only set if not already set by a real environment variable
                if (System.getenv(k) == null && System.getProperty(k) == null) {
                    System.setProperty(k, v);
                    log.debug(".env loaded: {}={}", k, v);
                }
            });
            log.info(".env file loaded from project root");
        } catch (Exception e) {
            log.warn(".env file found but could not be loaded: {}", e.getMessage());
        }
        loaded = true;
    }
}
