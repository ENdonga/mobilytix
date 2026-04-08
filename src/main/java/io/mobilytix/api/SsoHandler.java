package io.mobilytix.api;

import io.appium.java_client.android.AndroidDriver;
import io.mobilytix.config.AppConfig;
import io.mobilytix.core.DriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

/**
 * Handles SSO authentication flows.
 * <p>
 * SSO on Android typically follows one of two patterns:
 * 1. WebView — the app opens an in-app browser for the SSO provider login page
 * 2. Deep link — the SSO provider redirects back to the app via a custom URL scheme
 * <p>
 * Both patterns are stubbed here with clear extension points.
 * Implement the pattern that matches your app's SSO flow.
 * <p>
 * Common SSO providers: Google, Microsoft, Okta, Auth0, Keycloak.
 */
public class SsoHandler {
    private static final Logger log = LogManager.getLogger(SsoHandler.class);
    private static SsoHandler instance;

    private static final String CONTEXT_NATIVE = "NATIVE_APP";
    private static final String CONTEXT_WEBVIEW = "WEBVIEW";
    private static final int WEBVIEW_WAIT_MS = 3000;
    private static final int CONTEXT_POLL_MS = 500;
    private static final int CONTEXT_TIMEOUT_MS = 15000;

    private SsoHandler() {
    }

    public static synchronized SsoHandler getInstance() {
        if (instance == null) {
            instance = new SsoHandler();
        }
        return instance;
    }

    /**
     * Performs SSO login for the current app.
     *
     * @param username   SSO username or email
     * @param credential SSO password or token
     * @param appConfig  current app config
     */
    public void login(String username, String credential, AppConfig appConfig) {
        log.info("Starting SSO login | app: {} | user: {}", appConfig.getAppName(), username);
        // Switch to WebView context if the SSO flow uses an in-app browser
        if (switchToWebViewContent()) {
            performWebViewLogin(username, credential);
            switchToNativeContext();
        } else {
            // Fallback — SSO handled entirely in native context e.g. deep link redirect or native SSO SDK
            performNativeSsoLogin(username, credential);
        }
        log.info("SSO login complete for: {}", username);
    }

    /**
     * Switches the driver context to the first available WebView.
     * Waits up to CONTEXT_TIMEOUT_MS for a WebView context to appear.
     *
     * @return true if a WebView context was found and switched to
     */
    public boolean switchToWebViewContent() {
        log.debug("Waiting for WebView context...");
        AndroidDriver driver = DriverManager.getInstance().getDriver();

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < CONTEXT_TIMEOUT_MS) {
            Set<String> contexts = driver.getContextHandles();
            log.debug("Available contexts: {}", contexts);
            String webViewContext = contexts.stream().filter(ctx -> ctx.contains(CONTEXT_WEBVIEW)).findFirst().orElse(null);
            if (webViewContext != null) {
                driver.context(webViewContext);
                log.info("Switched to WebView context: {}", webViewContext);
                return true;
            }
            sleep(CONTEXT_POLL_MS);
        }
        log.warn("No WebView context found after {}ms — SSO may use native context only", CONTEXT_TIMEOUT_MS);
        return false;
    }

    /**
     * Switches the driver context back to the native app.
     */
    public void switchToNativeContext() {
        log.debug("Switching back to native context");
        DriverManager.getInstance().getDriver().context(CONTEXT_NATIVE);
        log.info("Switched to native context");
    }

    /**
     * Returns all currently available contexts.
     * Useful for debugging SSO flows.
     *
     * @return set of available context strings
     */
    public Set<String> getAvailableContexts() {
        return DriverManager.getInstance().getDriver().getContextHandles();
    }

    /**
     * Performs login inside a WebView context.
     * <p>
     * The driver is already switched to the WebView context when
     * this method is called. Use standard Selenium locators to
     * find and interact with the SSO provider's login form.
     *
     * @param username   SSO username
     * @param credential SSO password
     */
    private void performWebViewLogin(String username, String credential) {
        log.debug("Performing WebView SSO login for: {}", username);
        // TODO: implement with your SSO provider's login form locators
        // The driver is in WebView context here — use standard By locators
        // Example:
        //   driver().findElement(By.id("username")).sendKeys(username);
        //   driver().findElement(By.id("password")).sendKeys(credential);
        //   driver().findElement(By.id("submit")).click();
        throw new UnsupportedOperationException("WebView SSO login not implemented. Implement performWebViewLogin() with your provider's form locators.");
    }

    /**
     * Performs SSO login entirely in the native app context.
     * Use this when the app uses a native SSO SDK or deep link redirect
     * rather than an in-app WebView browser.
     *
     * @param username   SSO username
     * @param credential SSO password or token
     */
    private void performNativeSsoLogin(String username, String credential) {
        log.debug("Performing native SSO login for: {}", username);
        // TODO: implement with your app's native SSO page objects
        // Example:
        //   SsoLoginPage.getInstance().enterUsername(username);
        //   SsoLoginPage.getInstance().enterPassword(credential);
        //   SsoLoginPage.getInstance().tapSignIn();
        throw new UnsupportedOperationException(
                "Native SSO login not implemented. Implement performNativeSsoLogin() with your SSO page objects.");
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("SSO context polling interrupted");
        }
    }
}
