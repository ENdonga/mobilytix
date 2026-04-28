package io.mobilytix.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FrameworkConfig {
    @JsonProperty("apk_base_path")
    private String apkBasePath;
    @JsonProperty("appium")
    private AppiumConfig appium;
    @JsonProperty("timeouts")
    private TimeoutsConfig timeouts;
    @JsonProperty("execution_mode")
    private String executionMode = "local";
    @JsonProperty("sauce_labs")
    private SauceLabsConfig sauceLabs;
}
