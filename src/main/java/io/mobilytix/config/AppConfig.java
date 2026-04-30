package io.mobilytix.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents one app entry under the `apps:` section of config.yaml.
 * Fields map directly to the YAML keys.
 * Jackson populates this automatically via ConfigLoader.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {
    @JsonProperty("app_name")
    private String appName;
    @JsonProperty("apk_path")
    private String apkPath;
    @JsonProperty("package_name")
    private String packageName;
    @JsonProperty("activity")
    private String activity;
    @JsonProperty("requires_auth")
    private boolean requiresAuth;
    @JsonProperty("auth_type")
    private String authType;
    @JsonProperty("otp_source")
    private String otpSource;
    @JsonProperty("apk_source")
    private ApkSource apkSource;
}
