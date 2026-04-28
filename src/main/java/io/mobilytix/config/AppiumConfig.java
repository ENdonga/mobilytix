package io.mobilytix.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppiumConfig {
    @JsonProperty("host")
    private String host;
    @JsonProperty("port")
    private int port;
    @JsonProperty("auto_start")
    private boolean autoStart;
    @JsonProperty("log_level")
    private String logLevel;
}
