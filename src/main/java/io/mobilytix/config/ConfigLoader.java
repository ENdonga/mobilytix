package io.mobilytix.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.mobilytix.exceptions.ConfigException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.Map;

/**
 * Singleton configuration loader.
 * <p>
 * Reads config.yaml once on first access and caches the result.
 * All framework classes call ConfigLoader.getInstance() to get config values.
 * Never instantiate this class directly.
 * <p>
 * To add a new config value:
 * 1. Add the key to config.yaml
 * 2. Add a getter method here that calls getNestedValue() with the key path
 */
public class ConfigLoader {
    private static final Logger log = LogManager.getLogger(ConfigLoader.class);
    private static ConfigLoader instance;
    private final Map<String, Object> rawConfig;
    private final ObjectMapper mapper;

    // Config key constants - update here if config.yaml keys ever change
    private static final String KEY_FRAMEWORK = "framework";
    private static final String KEY_APPIUM = "appium";
    private static final String KEY_AUTO_START = "auto_start";
    private static final String KEY_HOST = "host";
    private static final String KEY_PORT = "port";
    private static final String KEY_LOG_LEVEL = "log_level";
    private static final String KEY_APK_BASE_PATH = "apk_base_path";
    private static final String KEY_TIMEOUTS = "timeouts";
    private static final String KEY_EXPLICIT = "explicit";
    private static final String KEY_PAGE_LOAD = "page_load";
    private static final String KEY_APPS = "apps";
    private static final String KEY_DEVICE = "device";
    private static final String KEY_REPORTING = "reporting";
    private static final String KEY_EXTENT = "extent";
    private static final String KEY_OUTPUT_PATH = "output_path";
    private static final String KEY_THEME = "theme";
    private static final String KEY_SCREENSHOTS = "screenshots";
    private static final String KEY_ON_FAILURE = "on_failure";
    private static final String KEY_ON_PASS = "on_pass";
    private static final String KEY_SCREEN_RECORDING = "screen_recording";
    private static final String KEY_ENABLED = "enabled";

    private ConfigLoader() {
        mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config/config.yml")) {
            if (is == null) {
                throw new RuntimeException("config.yaml not found. Ensure it exists at src/main/resources/config/config.yaml");
            }
            rawConfig = mapper.readValue(is, Map.class);
            log.info("config.yaml loaded successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config.yaml - " + e.getMessage(), e);
        }
    }

    /**
     * Returns the single instance of ConfigLoader.
     * Thread-safe via synchronized - safe for parallel test execution.
     */
    public static synchronized ConfigLoader getInstance() {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }

    // App config

    /**
     * Returns the AppConfig for the given app key.
     * The app key matches the key under `apps:` in config.yaml.
     * <p>
     * Example: getAppConfig("app_a") reads the `apps.app_a` block.
     *
     * @param appKey the key defined in config.yaml under apps:
     * @return populated AppConfig object
     * @throws RuntimeException if the key does not exist in config.yaml
     */
    @SuppressWarnings("unchecked")
    public AppConfig getAppConfig(String appKey) {
        try {
            Map<String, Object> apps = (Map<String, Object>) rawConfig.get(KEY_APPS);
            if (apps == null || !apps.containsKey(appKey)) {
                throw new ConfigException(appKey, "not found under apps: in config.yaml. " + "Available keys: " + apps.keySet());
            }
            String json = mapper.writeValueAsString(apps.get(appKey));
            AppConfig config = mapper.readValue(json, AppConfig.class);
            log.debug("AppConfig loaded for key '{}' - app: {}", appKey, config.getAppName());
            return config;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigException("Failed to load load config.yaml for key: " + appKey, e.getMessage());
        }
    }
    // Device config

    /**
     * Returns the DeviceConfig from the `device:` block in config.yaml.
     */
    @SuppressWarnings("unchecked")
    public DeviceConfig getDeviceConfig() {
        try {
            String json = mapper.writeValueAsString(rawConfig.get(KEY_DEVICE));
            return mapper.readValue(json, DeviceConfig.class);
        } catch (Exception e) {
            throw new ConfigException("Failed to load DeviceConfig");
        }
    }

    // Appium config
    public String getAppiumHost() {
        return getNestedValue(KEY_FRAMEWORK, KEY_APPIUM, KEY_HOST);
    }

    public int getAppiumPort() {
        return Integer.parseInt(getNestedValue(KEY_FRAMEWORK, KEY_APPIUM, KEY_PORT));
    }

    public boolean isAppiumAutoStart() {
        return Boolean.parseBoolean(getNestedValue(KEY_FRAMEWORK, KEY_APPIUM, KEY_AUTO_START));
    }

    public String getAppiumLogLevel() {
        return getNestedValue(KEY_FRAMEWORK, KEY_APPIUM, KEY_LOG_LEVEL).toLowerCase();
    }

    // Framework config
    public String getApkBasePath() {
        return getNestedValue(KEY_FRAMEWORK, KEY_APK_BASE_PATH);
    }

    public int getExplicitTimeout() {
        return Integer.parseInt(getNestedValue(KEY_FRAMEWORK, KEY_TIMEOUTS, KEY_EXPLICIT));
    }

    public int getPageLoadTimeout() {
        return Integer.parseInt(getNestedValue(KEY_FRAMEWORK, KEY_TIMEOUTS, KEY_PAGE_LOAD));
    }

    // Reporting config
    public String getExtentOutputPath() {
        return getNestedValue(KEY_REPORTING, KEY_EXTENT, KEY_OUTPUT_PATH);
    }

    public String getExtentTheme() {
        return getNestedValue(KEY_REPORTING, KEY_EXTENT, KEY_THEME).toUpperCase();
    }

    public boolean isScreenshotOnFailure() {
        return Boolean.parseBoolean(getNestedValue(KEY_REPORTING, KEY_SCREENSHOTS, KEY_ON_FAILURE));
    }

    public boolean isScreenshotOnPass() {
        return Boolean.parseBoolean(getNestedValue(KEY_REPORTING, KEY_SCREENSHOTS, KEY_ON_PASS));
    }

    public boolean isScreenRecordingEnabled() {
        return Boolean.parseBoolean(
                getNestedValue(KEY_REPORTING, KEY_SCREEN_RECORDING, KEY_ENABLED));
    }

    public String getScreenRecordingOutputPath() {
        return getNestedValue(KEY_REPORTING, KEY_SCREEN_RECORDING, KEY_OUTPUT_PATH);
    }

    // Internal helper

    /**
     * Traverses the raw config map by key path.
     * <p>
     * Example: getNestedValue("framework", "appium", "port")
     * reads rawConfig["framework"]["appium"]["port"]
     *
     * @param keys the sequence of keys to traverse
     * @return the value as a String
     */
    @SuppressWarnings("unchecked")
    public String getNestedValue(String... keys) {
        Object current = rawConfig;
        for (String key : keys) {
            if (!(current instanceof Map)) {
                throw new ConfigException(key, "parent is not a map - check config.yaml structure");
            }
            current = ((Map<String, Object>) current).get(key);
            if (current == null) {
                throw new ConfigException("Config key not found: '" + key + "' - check config.yaml");
            }
        }
        return String.valueOf(current).trim();
    }
}
