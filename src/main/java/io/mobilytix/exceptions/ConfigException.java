package io.mobilytix.exceptions;

public class ConfigException extends RuntimeException {
    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String key, String context) {
        super("Config key not found: '" + key + "' - " + context + ". Check config.yaml file");
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
