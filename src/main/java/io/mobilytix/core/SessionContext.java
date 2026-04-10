package io.mobilytix.core;

import io.mobilytix.config.AppConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Holds per-thread session state for the current test run.
 * <p>
 * Stores:
 * - The current app key (e.g. "app_a")
 * - The loaded AppConfig for that app
 * - Whether the session has been authenticated
 * <p>
 * All fields are ThreadLocal - safe for parallel test execution.
 * Each thread manages its own independent session state.
 * <p>
 * Lifecycle:
 * set() - called in BaseTest @BeforeClass after annotation is read
 * clear() - called in BaseTest @AfterClass during teardown
 */
public class SessionContext {
    private static final Logger log = LogManager.getLogger(SessionContext.class);
    private static final ThreadLocal<String> currentAppKey = new ThreadLocal<>();
    private static final ThreadLocal<AppConfig> currentAppConfig = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> authenticated = ThreadLocal.withInitial(() -> false);

    private SessionContext() {
    }

    /**
     * Sets the session state for the current thread.
     * Called once per test class in BaseTest @BeforeClass.
     *
     * @param appKey the app key from @AppUnderTest
     * @param appConfig the loaded config for that app
     */
    public static void set(String appKey, AppConfig appConfig) {
        currentAppKey.set(appKey.trim().toUpperCase());
        currentAppConfig.set(appConfig);
        authenticated.set(false);
        log.info("Session context has been set | thread: {} | app: {}", Thread.currentThread().getName(), appKey);
    }

    /**
     * Clears all session state for the current thread.
     * Called in BaseTest @AfterClass.
     * Prevents ThreadLocal memory leaks in thread pool environments.
     */
    public static void clear() {
        log.debug("Clearing session context for thread: {}", Thread.currentThread().getName());
        currentAppKey.remove();
        currentAppConfig.remove();
        authenticated.remove();
    }

    /**
     * Returns the app key for the current thread.
     *
     * @throws IllegalStateException if called before set()
     */
    public static String getAppKey() {
        String key = currentAppKey.get();
        if (key == null) {
            throw new IllegalStateException("Session context has not been initialized for thread: " + Thread.currentThread().getName() + ". Ensure BaseTest.setUp() was called");
        }
        return key;
    }

    /**
     * Returns the AppConfig for the current thread.
     *
     * @throws IllegalStateException if called before set()
     */
    public static AppConfig getAppConfig() {
        AppConfig config = currentAppConfig.get();
        if (config == null) {
            throw new IllegalStateException(
                    "AppConfig has not been initialised for thread: " + Thread.currentThread().getName() + ". Ensure BaseTest.setUp() was called.");
        }
        return config;
    }

    /**
     * Returns true if the current session has been authenticated.
     */
    public static boolean isAuthenticated() {
        return Boolean.TRUE.equals(authenticated.get());
    }

    /**
     * Marks the current session as authenticated.
     * Called by AuthHandler after a successful login.
     */
    public static void markAuthenticated() {
        authenticated.set(true);
        log.info("Session marked as authenticated for thread: {}", Thread.currentThread().getName());
    }

    /**
     * Resets the authentication state without clearing the full session.
     * Called by AppSwitcher when an app is reset to its initial state.
     */
    public static void resetAuthState() {
        authenticated.set(false);
        log.info("Auth state reset for thread: {}", Thread.currentThread().getName());
    }
}
