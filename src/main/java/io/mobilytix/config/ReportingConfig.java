package io.mobilytix.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportingConfig {
    @JsonProperty("extent")
    private ExtentConfig extent;
    @JsonProperty("screenshots")
    private ScreenshotsConfig screenshots;
    @JsonProperty("screen_recording")
    private ScreenRecordingConfig screenRecording;
}
