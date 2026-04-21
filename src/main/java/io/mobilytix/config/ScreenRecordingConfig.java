package io.mobilytix.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScreenRecordingConfig {
    @JsonProperty("enabled")
    private boolean enabled;
    @JsonProperty("output_path")
    private String outputPath;
}
