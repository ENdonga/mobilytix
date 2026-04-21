package io.mobilytix.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtentConfig {
    @JsonProperty("output_path")
    private String outputPath;
    @JsonProperty("theme")
    private String theme;
}
