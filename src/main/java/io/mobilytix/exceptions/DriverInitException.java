package io.mobilytix.exceptions;

public class DriverInitException extends RuntimeException {
    public DriverInitException(String appKey, Throwable cause) {
        super("Failed to initialize the driver for app: '" + appKey + "' - " + cause.getMessage(), cause);
    }
}
