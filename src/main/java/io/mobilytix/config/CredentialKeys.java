package io.mobilytix.config;

/**
 * Central registry of all credential environment variable keys.
 * <p>
 * All credential keys follow UPPER_SNAKE_CASE convention.
 * Values are resolved from .env file or CI environment variables
 * via ConfigLoader.getCredential().
 * <p>
 * To add credentials for a new app:
 * 1. Add constants here
 * 2. Add entries to .env.example with comments
 * 3. Call ConfigLoader.getCredential(CredentialKeys.YOUR_KEY, defaultValue)
 * in your test — no new getters needed
 * <p>
 * Never hardcode credential values in test code or config.yaml.
 */
public class CredentialKeys {
    private CredentialKeys() {
    }

    public static final String SAUCE_USERNAME = "SAUCE_USERNAME";
    public static final String SAUCE_PASSWORD = "SAUCE_PASSWORD";
    public static final String SAUCE_LOCKED_USERNAME = "SAUCE_LOCKED_USERNAME";

    // Sauce Labs cloud credentials — for CI execution on Sauce Labs RDC
    // Different from SAUCE_USERNAME/PASSWORD which are app test credentials
    public static final String SAUCE_LABS_USERNAME = "SAUCE_LABS_USERNAME";
    public static final String SAUCE_LABS_ACCESS_KEY = "SAUCE_LABS_ACCESS_KEY";

    /**
     * API token for email-based OTP resolution e.g. MailSlurp, Mailinator
     */
    public static final String OTP_API_TOKEN = "OTP_API_TOKEN";
    public static final String SSO_USERNAME = "SSO_USERNAME";
    public static final String SSO_PASSWORD = "SSO_PASSWORD";
}
