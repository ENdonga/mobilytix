package io.mobilytix.api;

import io.mobilytix.adb.AdbCommands;
import io.mobilytix.config.AppConfig;
import io.mobilytix.exceptions.AuthenticationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves OTP codes from configured sources.
 * <p>
 * Supported otp_source values (configured in config.yaml):
 * sms   — reads OTP from device SMS via ADB logcat
 * email — reads OTP from an email API (stub — implement with your provider)
 * totp  — generates OTP from a TOTP secret (stub — implement with a TOTP lib)
 * <p>
 * After resolving the OTP code, this class hands it to the page object
 * that handles OTP entry. Implement enterOtp() in your page object and
 * call it from the TODO comment below.
 */
public class OtpResolver {
    private static final Logger log = LogManager.getLogger(OtpResolver.class);
    private static OtpResolver instance;

    private final AdbCommands adb = AdbCommands.getInstance();

    private static final String OTP_SOURCE_SMS = "sms";
    private static final String OTP_SOURCE_EMAIL = "email";
    private static final String OTP_SOURCE_TOTP = "totp";

    // SMS polling
    private static final int SMS_POLL_INTERVAL_MS = 2000;
    private static final int SMS_TIMEOUT_MS = 30000;
    private static final String LOGCAT_TAG_SMS = "SmsRetriever";
    private static final int LOGCAT_LINE_COUNT = 50;

    // OTP pattern — matches 4 to 8 consecutive digits. Adjust this pattern to match your app's OTP format
    private static final String OTP_REGEX = "\\b(\\d{4,8})\\b";
    private static final Pattern OTP_PATTERN = Pattern.compile(OTP_REGEX);

    private OtpResolver() {
    }

    public static synchronized OtpResolver getInstance() {
        if (instance == null) {
            instance = new OtpResolver();
        }
        return instance;
    }

    /**
     * Resolves an OTP code from the configured source and enters it
     * into the app via the appropriate page object.
     *
     * @param username  the user identifier
     * @param secret    OTP seed, email API token, or TOTP secret key
     * @param appConfig the current app config — used to read otp_source
     */
    public void resolve(String username, String secret, AppConfig appConfig) {
        String otpSource = appConfig.getOtpSource().trim().toLowerCase();
        log.info("Resolving OTP | source: {} | user: {}", otpSource, username);
        String otpCode = switch (otpSource) {
            case OTP_SOURCE_SMS -> resolveFromSms(appConfig.getPackageName());
            case OTP_SOURCE_EMAIL -> resolveFromEmail(secret);
            case OTP_SOURCE_TOTP -> resolveFromTotp(secret);
            default -> throw new AuthenticationException("otp", "unknown otp_source: '" + otpSource + "'. Valid values: sms, email, totp.");
        };
        log.info("OTP resolved successfully for user: {} [code redacted]", username);
        //TODO: pass otpCode to your OTP entry page object here
        // Example: OtpPage.getInstance().enterOtp(otpCode);
        log.debug("OTP entry not wired to a page object yet. " +
                "Implement OtpPage and call it here.");
    }

    /**
     * Reads OTP from device SMS via ADB logcat.
     * <p>
     * Works for apps using the Android SMS Retriever API.
     * Clears the logcat buffer first to avoid reading stale messages,
     * then polls until the OTP appears or the timeout is reached.
     *
     * @param packageName the app package — used to scope the logcat filter
     * @return the resolved OTP code as a string
     */
    private String resolveFromSms(String packageName) {
        log.debug("Waiting for OTP via SMS logcat | package: {}", packageName);
        adb.clearLogcat();
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < SMS_TIMEOUT_MS) {
            String logcat = adb.getLogcat(LOGCAT_TAG_SMS, LOGCAT_LINE_COUNT);
            String otp = extractOtpFromLogcat(logcat);
            if (otp != null) {
                log.debug("OTP entry found in SMS logcat");
            }
            sleep(SMS_POLL_INTERVAL_MS);
        }
        throw new AuthenticationException("otp/sms", "OTP not received via SMS within " + (SMS_TIMEOUT_MS / 1000) + " seconds. " +
                "Check that the SMS was sent and logcat tag '" + LOGCAT_TAG_SMS + "' is correct.");
    }

    /**
     * Resolves OTP from an email inbox via an external API.
     * <p>
     * Stub — implement with your email testing provider.
     * Recommended providers: Mailinator, MailSlurp, Mailtrap.
     *
     * @param emailApiToken API token or inbox identifier for your email provider
     * @return the resolved OTP code
     */
    private String resolveFromEmail(String emailApiToken) {
        log.debug("Resolving OTP from email");
        // TODO: implement with your email provider
        // Example using MailSlurp:
        // MailslurpClient client = new MailslurpClient(emailApiToken);
        // Email email = client.waitForLatestEmail(inboxId, timeout);
        // return extractOtpFromText(email.getBody());
        throw new UnsupportedOperationException("Email OTP resolution not implemented. Implement resolveFromEmail() in OtpResolver with your email provider.");
    }

    /**
     * Generates a TOTP code from a base32 secret key.
     * <p>
     * Stub — implement with a TOTP library.
     * Recommended library: dev.samstevens.totp:totp (add to pom.xml).
     *
     * @param totpSecret the base32 TOTP secret key
     * @return the current TOTP code
     */
    private String resolveFromTotp(String totpSecret) {
        log.debug("Generating TOTP code");
        // TODO: implement with a TOTP library
        // Example using dev.samstevens.totp:
        // TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();
        // return totp.generateOneTimePassword(totpSecret, Instant.now());
        throw new UnsupportedOperationException("TOTP generation not implemented. Add dev.samstevens.totp:totp to pom.xml and implement resolveFromTotp().");
    }

    /**
     * Extracts an OTP code from a text string using the configured regex.
     * Returns null if no match is found.
     * <p>
     * Adjust OTP_REGEX if your app uses a different OTP format
     * e.g. alphanumeric codes, fixed length, different separators.
     *
     * @param text the text to search e.g. logcat output or email body
     * @return the first matching OTP code or null
     */
    private String extractOtpFromLogcat(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        Matcher matcher = OTP_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("OTP polling interrupted");
        }
    }
}
