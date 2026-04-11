package io.mobilytix.core;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.mobilytix.config.ConfigLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

/**
 * Manages the Appium server lifecycle.
 * <p>
 * Behaviour is controlled by framework.appium.auto_start in config.yaml:
 * auto_start: true - framework starts and stops the server automatically
 * auto_start: false - framework assumes server is already running externally
 * <p>
 * In both modes, a port check is performed before any start attempt to prevent
 * double-start collisions when a server is already running on the configured port.
 */
public class AppiumServerManager {
    private static final Logger log = LogManager.getLogger(AppiumServerManager.class);
    private static AppiumServerManager instance;
    private AppiumDriverLocalService service;
    private final ConfigLoader config = ConfigLoader.getInstance();

    private static final String STATUS_ENDPOINT = "/status";
    private static final String REQUEST_METHOD = "GET";
    private static final int CONNECTION_TIMEOUT_MS = 2000;
    private static final int SERVER_START_TIMEOUT_SECONDS = 60;
    private static final int SERVER_READY_MAX_ATTEMPTS = 5;
    private static final int SERVER_READY_POLL_MS = 1000;

    private AppiumServerManager() {
    }

    /**
     * Returns the singleton instance.
     * Synchronized to be safe during parallel suite initialisation.
     */
    public static synchronized AppiumServerManager getInstance() {
        if (instance == null) {
            instance = new AppiumServerManager();
        }
        return instance;
    }

    /**
     * Starts the Appium server if auto_start is enabled and no server is currently running on the configured port.
     * <p>
     * Called from BaseTest @BeforeSuite.
     * Safe to call multiple times - subsequent calls are no-ops.
     */
    public void startIfRequired() {
        if (!config.isAppiumAutoStart()) {
            log.info("Appium auto_start=false - skipping server start. Ensure Appium is running manually on {}:{}", config.getAppiumHost(), config.getAppiumPort());
            return;
        }
        if (isServerRunning()) {
            log.info("Appium server is already running on port - skipping start {}", config.getAppiumPort());
            return;
        }
        log.info("Starting Appium Server on {}:{}", config.getAppiumHost(), config.getAppiumPort());
        service = buildService();
        service.start();
        waitForAppiumServerReady();
        log.info("Appium Server started successfully. URL {}", service.getUrl());
    }

    /**
     * Stops the Appium server if it was started by this manager.
     * Called from BaseTest @AfterSuite.
     * If auto_start=false this is a no-op.
     */
    public void stop() {
        if (service != null && service.isRunning()) {
            log.info("Stopping Appium Server in 3,2,1...");
            service.stop();
            log.info("Appium Server stopped successfully");
        } else {
            log.debug("Could not managed stopping Appium server. Investigate this.");
        }
    }

    /**
     * Returns the URL of the Appium server. Works regardless of whether the server was started by this manager
     * or externally - always constructs from config values.
     */
    public URL getServiceUrl() {
        if (service != null && service.isRunning()) {
            return service.getUrl();
        }
        try {
            return new URL("http://" + config.getAppiumHost() + ":" + config.getAppiumPort());
        } catch (Exception e) {
            throw new RuntimeException("Invalid Appium Server URL in the config", e);
        }
    }

    /**
     * Builds the Appium service with config values.
     */
    private AppiumDriverLocalService buildService() {
        return new AppiumServiceBuilder()
                .withIPAddress(config.getAppiumHost())
                .usingPort(config.getAppiumPort())
                .withArgument(GeneralServerFlag.LOG_LEVEL, config.getAppiumLogLevel())
                .withArgument(GeneralServerFlag.RELAXED_SECURITY)
                .withTimeout(Duration.ofSeconds(SERVER_START_TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Checks if an Appium server is already running on the configured port by hitting the /status endpoint.
     * <p>
     * Returns true if the server responds with HTTP 200.
     * Returns false for any connection failure or non-200 response.
     */
    public boolean isServerRunning() {
        String statusUrl = "http://" + config.getAppiumHost() + ":" + config.getAppiumPort() + STATUS_ENDPOINT;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(statusUrl).openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            connection.setReadTimeout(CONNECTION_TIMEOUT_MS);
            connection.setRequestMethod(REQUEST_METHOD);
            int responseCode = connection.getResponseCode();
            boolean running = responseCode == HttpURLConnection.HTTP_OK;
            log.debug("Appium Server status check at: {} - response: {} - running: {}", statusUrl, responseCode, running);
            return running;
        } catch (IOException e) {
            log.error("Appium server status check failed (server is not running): {}", e.getMessage());
            return false;
        }
    }

    /**
     * Polls the /status endpoint until the server responds or timeout is reached.
     * Prevents driver initialisation from running before server is ready.
     */
    private void waitForAppiumServerReady() {
        log.debug("Waiting for Appium server to be ready...");
        int attempts = 0;
        while (attempts < SERVER_READY_MAX_ATTEMPTS) {
            if (isServerRunning()) {
                log.info("Appium server ready after {} attempt(s)", attempts + 1);
                return;
            }
            attempts++;
            log.debug("Server not ready yet — attempt {}/{}", attempts, SERVER_READY_MAX_ATTEMPTS);
            sleep(SERVER_READY_POLL_MS);
        }
        throw new RuntimeException("Appium server did not become ready after " + SERVER_READY_MAX_ATTEMPTS + " attempts. Check if port " + config.getAppiumPort() + " is available.");
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
