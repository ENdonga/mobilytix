package io.mobilytix.exceptions;

public class AdbCommandException extends RuntimeException {
    public AdbCommandException(String message) {
        super(message);
    }

    public AdbCommandException(String command, Throwable cause) {
        super("ADB command failed: '" + command + "' — " + cause.getMessage(), cause);
    }
}
