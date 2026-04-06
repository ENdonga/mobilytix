package io.mobilytix.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents the `device:` section of config.yaml.
 * Jackson populates this automatically via ConfigLoader.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceConfig {
    @JsonProperty("udid")
    private String udid;
    @JsonProperty("platform_name")
    private String platformName;
    @JsonProperty("platform_version")
    private String platformVersion;
    @JsonProperty("automation_name")
    private String automationName;
    @JsonProperty("new_command_timeout")
    private long newCommandTimeout;
    @JsonProperty("no_reset")
    private boolean noReset;
    @JsonProperty("full_reset")
    private boolean fullReset;
}
