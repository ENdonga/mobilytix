package io.mobilytix.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScreenshotsConfig {
    @JsonProperty("on_failure")
    private boolean onFailure;
    @JsonProperty("on_pass")
    private boolean onPass;
}
