package io.mobilytix.exceptions;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String authType, String reason) {
        super("Authentication failed | type: " + authType + " | reason: " + reason);
    }

    public AuthenticationException(String authType, Throwable cause) {
        super("Authentication failed | type: " + authType + " | " + cause.getMessage(), cause);
    }
}
