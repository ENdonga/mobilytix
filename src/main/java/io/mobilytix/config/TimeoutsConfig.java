package io.mobilytix.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeoutsConfig {
    @JsonProperty("explicit")
    private int explicit;

    @JsonProperty("implicit")
    private int implicit;

    @JsonProperty("page_load")
    private int pageLoad;
}
