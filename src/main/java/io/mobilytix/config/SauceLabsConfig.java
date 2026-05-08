package io.mobilytix.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SauceLabsConfig {
    @JsonProperty("region")
    private String region = "eu-central-1";

    @JsonProperty("build")
    private String build = "mobilytix";

    @JsonProperty("device_name")
    private String deviceName;

    @JsonProperty("platform_version")
    private String platformVersion;

    @JsonProperty("app_storage_filename")
    private String appStorageFilename;
}
