package io.mobilytix.exceptions;

public class AppiumServerException extends RuntimeException {
    public AppiumServerException(String message) {
        super(message);
    }

    public AppiumServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
