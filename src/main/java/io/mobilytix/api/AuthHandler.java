package io.mobilytix.api;

import io.mobilytix.config.AppConfig;
import io.mobilytix.core.SessionContext;
import io.mobilytix.exceptions.AuthenticationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Single entry point for all authentication flows.
 * <p>
 * Reads auth_type from the current app's config and delegates
 * to the appropriate handler. Idempotent — calling authenticate()
 * on an already-authenticated session is a no-op.
 * <p>
 * Supported auth types (configured in config.yaml):
 * none  — no authentication required
 * basic — username and password
 * otp   — OTP via SMS, email, or TOTP
 * sso   — Single Sign-On via WebView or deep link
 * <p>
 * Usage:
 * AuthHandler.getInstance().authenticate("user@example.com", "password");
 */
public class AuthHandler {
    private static final Logger log = LogManager.getLogger(AuthHandler.class);
    private static AuthHandler instance;

    private static final String AUTH_TYPE_NONE = "none";
    private static final String AUTH_TYPE_BASIC = "basic";
    private static final String AUTH_TYPE_OTP = "otp";
    private static final String AUTH_TYPE_SSO = "sso";

    private AuthHandler() {
    }

    public static AuthHandler getInstance() {
        if (instance == null) {
            instance = new AuthHandler();
        }
        return instance;
    }

    /**
     * Authenticates the current app session based on the auth_type
     * configured in config.yaml for the current app.
     * <p>
     * Idempotent — if the session is already authenticated this
     * method logs and returns immediately without any action.
     *
     * @param username   user identifier — email, phone, or username
     * @param credential password, OTP seed, or SSO token depending on auth_type
     */
    public void authenticate(String username, String credential) {
        AppConfig appConfig = SessionContext.getAppConfig();
        if (!appConfig.isRequiresAuth()) {
            log.info("App '{}' does not require authentication — skipping", appConfig.getAppName());
            return;
        }
        if (SessionContext.isAuthenticated()) {
            log.info("App '{}' is already authenticated, skipping authentication", appConfig.getAppName());
            return;
        }
        String authType = appConfig.getAuthType().trim().toLowerCase();
        log.info("Authenticating | app: {} | type: {} | user: {}", appConfig.getAppName(), authType, username);
        switch (authType) {
            case AUTH_TYPE_NONE -> handleNone();
            case AUTH_TYPE_BASIC -> handleBasic(username, credential);
            case AUTH_TYPE_OTP -> handleOtp(username, credential, appConfig);
            case AUTH_TYPE_SSO -> handleSso(username, credential, appConfig);
            default -> throw new AuthenticationException(
                    authType, "unknown auth_type. Valid values: none, basic, otp, sso. Check auth_type in config.yaml for app: " + appConfig.getAppName());
        }
        SessionContext.markAuthenticated();
        log.info("Authentication successful | user: {}", username);
    }

    /**
     * Checks if authentication is required for the current app.
     * Useful for conditional setup in test classes.
     *
     * @return true if the current app requires authentication
     */
    public boolean isAuthRequired() {
        return SessionContext.getAppConfig().isRequiresAuth();
    }

    private void handleNone() {
        log.info("auth_type=none — no authentication action required");
    }

    /**
     * Handles basic username + password authentication.
     * Implement by calling your LoginPage object here.
     * <p>
     * Example:
     * LoginPage.getInstance().login(username, credential);
     */
    private void handleBasic(String username, String credential) {
        log.debug("Handling basic auth for: {}", username);
        // TODO: call your app's LoginPage here
        throw new UnsupportedOperationException("Basic auth handler not implemented. Implement handleBasic() in AuthHandler by calling your LoginPage.");
    }

    private void handleOtp(String username, String credential, AppConfig appConfig) {
        log.debug("Delegating to OtpResolver for: {}", username);
        OtpResolver.getInstance().resolve(username, credential, appConfig);
    }

    private void handleSso(String username, String credential, AppConfig appConfig) {
        log.debug("Delegating to SsoHandler for: {}", username);
        SsoHandler.getInstance().login(username, credential, appConfig);
    }
}
