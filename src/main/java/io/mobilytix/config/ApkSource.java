package io.mobilytix.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApkSource {
    @JsonProperty("base_url")
    private String baseUrl;

    @JsonProperty("version")
    private String version;

    @JsonProperty("build")
    private String build;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("download_url")
    private String downloadUrl;

    /**
     * Returns the resolved download URL.
     * Uses full_url if set, otherwise constructs from parts.
     * Filename supports {version} and {build} placeholders.
     */
    public String resolveDownloadUrl() {
        if (downloadUrl != null && !downloadUrl.isBlank()) {
            return downloadUrl;
        }
        String resolvedFilename = filename.replace("{version}", version).replace("{build}", build);
        return baseUrl + "/" + version + "/" + resolvedFilename;
    }

    /**
     * Returns a cache-friendly version string combining version and build.
     * e.g. "2.2.0-25"
     */
    public String getCacheKey() {
        return version + "-" + build;
    }
}
