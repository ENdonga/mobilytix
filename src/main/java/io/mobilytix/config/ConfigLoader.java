package io.mobilytix.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.mobilytix.exceptions.ConfigException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Singleton configuration loader.
 * <p>
 * Reads config.yaml once on first access and caches the result.
 * All framework classes call ConfigLoader.getInstance() to get config values.
 * <p>
 * Override priority (highest to lowest):
 * 1. System property  -Dprop.key=value   (command line)
 * 2. System property  ENV_KEY=value       (.env file loaded by EnvLoader)
 * 3. OS env var       ENV_KEY=value       (shell or CI environment)
 * 4. config.yaml                          (default fallback)
 * <p>
 * See docs/ENV_OVERRIDE_GUIDE.md for the full list of override keys.
 */
public class ConfigLoader {
    private static final Logger log = LogManager.getLogger(ConfigLoader.class);
    private static ConfigLoader instance;
    private final Map<String, Object> rawConfig;
    private final ObjectMapper mapper;

    private final FrameworkConfig framework;
    private final DeviceConfig device;
    private final ReportingConfig reporting;

    // Config key constants - update here if config.yaml keys ever change
    private static final String KEY_FRAMEWORK = "framework";
    private static final String KEY_APPS = "apps";
    private static final String KEY_DEVICE = "device";
    private static final String KEY_REPORTING = "reporting";
    private static final String KEY_PARALLEL_DEVICES = "parallel_devices";

    /**
     * Override key constants
     * Prop keys  = dot-notation used with -D flag
     * Env keys   = UPPER_SNAKE used in .env file and OS environment
     */
    // Device
    private static final String PROP_DEVICE_UDID = "device.udid";
    private static final String ENV_DEVICE_UDID = "DEVICE_UDID";
    private static final String PROP_DEVICE_PLATFORM_VERSION = "device.platform_version";
    private static final String ENV_DEVICE_PLATFORM_VERSION = "DEVICE_PLATFORM_VERSION";
    // Appium
    private static final String PROP_APPIUM_HOST = "appium.host";
    private static final String ENV_APPIUM_HOST = "APPIUM_HOST";
    private static final String PROP_APPIUM_PORT = "appium.port";
    private static final String ENV_APPIUM_PORT = "APPIUM_PORT";
    private static final String PROP_APPIUM_AUTO_START = "appium.auto_start";
    private static final String ENV_APPIUM_AUTO_START = "APPIUM_AUTO_START";
    private static final String PROP_EXECUTION_MODE_OVERRIDE = "execution_mode";
    private static final String ENV_EXECUTION_MODE_OVERRIDE = "EXECUTION_MODE";
    // Reporting
    private static final String PROP_EXTENT_OUTPUT_PATH = "extent.output_path";
    private static final String ENV_EXTENT_OUTPUT_PATH = "EXTENT_OUTPUT_PATH";
    private static final String PROP_SCREEN_RECORDING_ENABLED = "screen.recording.enabled";
    private static final String ENV_SCREEN_RECORDING_ENABLED = "SCREEN_RECORDING_ENABLED";
    // Authentication — credentials only come from env, never config.yaml
    private static final String ENV_OTP_API_TOKEN = "OTP_API_TOKEN";
    private static final String ENV_SSO_USERNAME = "SSO_USERNAME";
    private static final String ENV_SSO_PASSWORD = "SSO_PASSWORD";

    private ConfigLoader() {
        mapper = new ObjectMapper(new YAMLFactory());
        // Step 1 — read the config.yml file. Failure here means: file not found or invalid YAML
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config/config.yml")) {
            if (is == null) {
                throw new ConfigException("config.yaml not found. Ensure it exists at src/main/resources/config/config.yaml");
            }
            rawConfig = mapper.readValue(is, Map.class);
            log.info("config.yaml loaded successfully");
        } catch (ConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigException("Failed to load config.yaml — " + e.getMessage());
        }

        // Step 2 - map raw config to typed models. Failure here means: config.yaml structure does not match the model class
        try {
            device = mapper.convertValue(rawConfig.get(KEY_DEVICE), DeviceConfig.class);
            framework = mapper.convertValue(rawConfig.get(KEY_FRAMEWORK), FrameworkConfig.class);
            reporting = mapper.convertValue(rawConfig.get(KEY_REPORTING), ReportingConfig.class);
        } catch (Exception e) {
            throw new ConfigException("Failed to map config.yaml to model — " + e.getMessage());
        }
        resolveAllOverrides();
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

    /**
     * Re-applies all overrides from system properties and environment variables.
     * Call this after EnvLoader.load() to pick up .env file values that were
     * not available when ConfigLoader was first instantiated.
     */
    public void applyOverrides() {
        resolveAllOverrides();
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
                throw new ConfigException(appKey, "not found under apps: in config.yaml. " + "Available keys: " + (apps != null ? apps.keySet() : "none"));
            }
//            String json = mapper.writeValueAsString(apps.get(appKey));
            AppConfig config = mapper.convertValue(apps.get(appKey), AppConfig.class);
            log.debug("AppConfig loaded for key '{}' — app: {}", appKey, config.getAppName());
            return config;
        } catch (ConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigException("Failed to load config for key: " + appKey);
        }
    }

    // Device config
    public String getExecutionMode() {
        return framework.getExecutionMode();
    }

    public SauceLabsConfig getSauceLabsConfig() {
        return framework.getSauceLabs();
    }

    public boolean isRunningOnSauceLabs() {
        return "sauce_labs".equals(framework.getExecutionMode());
    }

    /**
     * Returns the DeviceConfig from the device: block in config.yaml.
     * Applies overrides from system properties and environment variables.
     */
    @SuppressWarnings("unchecked")
    public DeviceConfig getDeviceConfig() {
        return device;
    }

    // Appium config
    public String getAppiumHost() {
        return framework.getAppium().getHost();
    }

    public int getAppiumPort() {
        return framework.getAppium().getPort();
    }

    public boolean isAppiumAutoStart() {
        return framework.getAppium().isAutoStart();
    }

    public String getAppiumLogLevel() {
        return framework.getAppium().getLogLevel().toLowerCase();
    }

    // Framework config
    public String getApkBasePath() {
        return framework.getApkBasePath();
    }

    public int getExplicitTimeout() {
        return framework.getTimeouts().getExplicit();
    }

    public int getPageLoadTimeout() {
        return framework.getTimeouts().getPageLoad();
    }

    // Reporting config
    public String getExtentOutputPath() {
        return reporting.getExtent().getOutputPath();
    }

    public String getExtentTheme() {
        return reporting.getExtent().getTheme();
    }

    public boolean isScreenshotOnFailure() {
        return reporting.getScreenshots().isOnFailure();
    }

    public boolean isScreenshotOnPass() {
        return reporting.getScreenshots().isOnPass();
    }

    public boolean isScreenRecordingEnabled() {
        return reporting.getScreenRecording().isEnabled();
    }

    public String getScreenRecordingOutputPath() {
        return reporting.getScreenRecording().getOutputPath();
    }

    /**
     * Returns the OTP API token for email-based OTP resolution.
     * Set via OTP_API_TOKEN in .env or CI environment.
     *
     * @return token string or null if not configured
     */
    public String getOtpApiToken() {
        String token = System.getProperty(ENV_OTP_API_TOKEN, System.getenv(ENV_OTP_API_TOKEN));
        if (token == null || token.isBlank()) {
            log.warn("OTP_API_TOKEN not set — email OTP resolution will fail. " + "Set it in .env or as a CI environment variable.");
        }
        return token;
    }

    /**
     * Returns the SSO username.
     * Set via SSO_USERNAME in .env or CI environment.
     *
     * @return username string or null if not configured
     */
    public String getSsoUsername() {
        return System.getProperty(ENV_SSO_USERNAME, System.getenv(ENV_SSO_USERNAME));
    }

    /**
     * Returns the SSO password.
     * Set via SSO_PASSWORD in .env or CI environment.
     *
     * @return password string or null if not configured
     */
    public String getSsoPassword() {
        return System.getProperty(ENV_SSO_PASSWORD, System.getenv(ENV_SSO_PASSWORD));
    }

    @SuppressWarnings("unchecked")
    public List<String> getParallelDeviceUdids() {
        try {
            List<Map<String, Object>> devices = (List<Map<String, Object>>) rawConfig.get(KEY_PARALLEL_DEVICES);
            if (devices == null || devices.isEmpty()) {
                return List.of();
            }
            return devices.stream()
                    .map(device -> String.valueOf(device.get("udid")))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Traverses the raw config map by key path.
     * <p>
     * Example: getNestedValue("framework", "appium", "port")
     * reads rawConfig["framework"]["appium"]["port"]
     *
     * @param keys the sequence of keys to traverse
     * @return the value as a trimmed String
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
                throw new ConfigException("Config: '" + key + "' key not found - check config.yaml");
            }
        }
        return String.valueOf(current).trim();
    }

    /**
     * Resolves a credential value from three sources in priority order:
     * 1. System property  -Dkey=value   (command line)
     * 2. System property  KEY=value     (.env file loaded by EnvLoader)
     * 3. OS env var       KEY=value     (CI environment)
     * 4. defaultValue                   (fallback — use only for non-sensitive values)
     * <p>
     * To add support for a new credential:
     * 1. Add a constant to CredentialKeys
     * 2. Add the key to .env.example
     * 3. Call this method with the constant — no new getter needed
     *
     * @param envKey       the key from CredentialKeys e.g. CredentialKeys.SAUCE_USERNAME
     * @param defaultValue fallback if not set — pass null for sensitive credentials that must always come from the environment
     * @return the resolved credential value or defaultValue
     */
    public String getCredential(String envKey, String defaultValue) {
        // Check command line -Denvkey=value
        String fromProp = System.getProperty(envKey);
        if (fromProp != null && !fromProp.isBlank()) return fromProp.trim();

        // Check OS environment variable
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();

        if (defaultValue != null) {
            log.debug("Credential '{}' not set — using default", envKey);
            return defaultValue;
        }

        log.warn("Credential '{}' not configured. Set it in .env or CI environment.", envKey);
        return null;
    }

    /**
     * Resolves an override value from three sources in priority order:
     * 1. System property  -Dprop.key=value  (command line)
     * 2. System property  ENV_KEY=value      (.env file via EnvLoader)
     * 3. OS env var       ENV_KEY=value      (shell or CI)
     *
     * @param propKey dot-notation system property key e.g. "device.udid"
     * @param envKey  uppercase env var key            e.g. "DEVICE_UDID"
     * @return the override value or null if not set in any source
     */
    private String resolveOverride(String propKey, String envKey) {
        // 1. Command line: -Dprop.key=value
        String fromProp = System.getProperty(propKey);
        if (fromProp != null && !fromProp.isBlank()) return fromProp.trim();

        // 2. .env file — EnvLoader stores as System.setProperty(ENV_KEY, value)
        String fromEnvProp = System.getProperty(envKey);
        if (fromEnvProp != null && !fromEnvProp.isBlank()) return fromEnvProp.trim();

        // 3. OS environment variable
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();

        return null;
    }

    /**
     * Returns a human-readable label for which source provided the override.
     * Used in log messages only.
     */
    private String getOverrideSource(String propKey, String envKey) {
        if (System.getProperty(propKey) != null) {
            return "command line (-D" + propKey + ")";
        }
        if (System.getProperty(envKey) != null) {
            return ".env file";
        }
        if (System.getenv(envKey) != null) {
            return "OS environment variable";
        }
        return "config.yaml";
    }

    private void resolveAllOverrides() {
        // Execution mode - local/Sauce Labs
        String executionModeOverride = resolveOverride(PROP_EXECUTION_MODE_OVERRIDE, ENV_EXECUTION_MODE_OVERRIDE);
        if (executionModeOverride != null && !executionModeOverride.equals(framework.getExecutionMode())) {
            log.info("Execution mode overridden to: {}", executionModeOverride);
            framework.setExecutionMode(executionModeOverride);
        }
        // ---- Appium ----
        String hostOverride = resolveOverride(PROP_APPIUM_HOST, ENV_APPIUM_HOST);
        if (hostOverride != null && !hostOverride.equals(framework.getAppium().getHost())) {
            log.info("Appium host overridden: {} → {} (source: {})", framework.getAppium().getHost(),
                    hostOverride, getOverrideSource(PROP_APPIUM_HOST, ENV_APPIUM_HOST));
            framework.getAppium().setHost(hostOverride);
        }

        String portOverride = resolveOverride(PROP_APPIUM_PORT, ENV_APPIUM_PORT);
        if (portOverride != null && Integer.parseInt(portOverride) != framework.getAppium().getPort()) {
            log.info("Appium port overridden: {} → {} (source: {})", framework.getAppium().getPort(), portOverride,
                    getOverrideSource(PROP_APPIUM_PORT, ENV_APPIUM_PORT));
            framework.getAppium().setPort(Integer.parseInt(portOverride));
        }

        String autoStartOverride = resolveOverride(PROP_APPIUM_AUTO_START, ENV_APPIUM_AUTO_START);
        if (autoStartOverride != null && Boolean.parseBoolean(autoStartOverride) != framework.getAppium().isAutoStart()) {
            log.info("Appium auto_start overridden: {} → {} (source: {})", framework.getAppium().isAutoStart(),
                    autoStartOverride, getOverrideSource(PROP_APPIUM_AUTO_START, ENV_APPIUM_AUTO_START));
            framework.getAppium().setAutoStart(Boolean.parseBoolean(autoStartOverride));
        }

        // ---- Device ----
        String udidOverride = resolveOverride(PROP_DEVICE_UDID, ENV_DEVICE_UDID);
        if (udidOverride != null && !udidOverride.equals(device.getUdid())) {
            if (log.isInfoEnabled()) {
                log.info("Device UDID overridden: {} → {} (source: {})", device.getUdid(), udidOverride, getOverrideSource(PROP_DEVICE_UDID, ENV_DEVICE_UDID));
            }
            device.setUdid(udidOverride);
        } else if (udidOverride != null) {
            log.debug("Device UDID override matches config value — no change: {}", udidOverride);
        }

        String platformOverride = resolveOverride(PROP_DEVICE_PLATFORM_VERSION, ENV_DEVICE_PLATFORM_VERSION);
        if (platformOverride != null && !platformOverride.equals(device.getPlatformVersion())) {
            log.info("Device platform version overridden: {} → {} (source: {})", device.getPlatformVersion(), platformOverride,
                    getOverrideSource(PROP_DEVICE_PLATFORM_VERSION, ENV_DEVICE_PLATFORM_VERSION));
            device.setPlatformVersion(platformOverride);
        }

        // ---- Reporting ----
        String extentPathOverride = resolveOverride(PROP_EXTENT_OUTPUT_PATH, ENV_EXTENT_OUTPUT_PATH);
        if (extentPathOverride != null && !extentPathOverride.equals(reporting.getExtent().getOutputPath())) {
            log.info("Extent output path overridden to: {} (source: {})", extentPathOverride,
                    getOverrideSource(PROP_EXTENT_OUTPUT_PATH, ENV_EXTENT_OUTPUT_PATH));
            reporting.getExtent().setOutputPath(extentPathOverride);
        }

        String screenRecordOverride = resolveOverride(PROP_SCREEN_RECORDING_ENABLED, ENV_SCREEN_RECORDING_ENABLED);
        if (screenRecordOverride != null && Boolean.parseBoolean(screenRecordOverride) != reporting.getScreenRecording().isEnabled()) {
            log.info("Screen recording enabled overridden to: {} (source: {})", screenRecordOverride,
                    getOverrideSource(PROP_SCREEN_RECORDING_ENABLED, ENV_SCREEN_RECORDING_ENABLED));
            reporting.getScreenRecording().setEnabled(Boolean.parseBoolean(screenRecordOverride));
        }
    }
}
