package io.mobilytix.utils;

import io.appium.java_client.android.AndroidDriver;
import io.mobilytix.config.ConfigLoader;
import io.mobilytix.core.DriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Centralised explicit wait strategies for the framework.
 * <p>
 * All waiting in the framework goes through this class.
 * Never call Thread.sleep() in page objects - use waitForCondition() or one of the typed methods here instead.
 * <p>
 * Default timeout is read from config.yaml framework.timeouts.explicit.
 * Individual calls can override the timeout by passing a seconds value.
 */
public class WaitUtils {
    private static final Logger log = LogManager.getLogger(WaitUtils.class);
    private static final int DEFAULT_TIMEOUT = ConfigLoader.getInstance().getExplicitTimeout();
    private static final int SHORT_TIMEOUT = 3; // seconds
    private static final int POLLING_INTERVAL_MS = 500;

    private WaitUtils() {
    }

    /**
     * Waits for an element to be visible using the default timeout.
     *
     * @param locator the By locator for the element
     * @return the visible WebElement
     */
    public static WebElement waitForVisible(By locator) {
        return waitForVisible(locator, DEFAULT_TIMEOUT);
    }

    /**
     * Waits for an element to be visible using a custom timeout.
     *
     * @param locator        the By locator for the element
     * @param timeoutSeconds custom timeout in seconds
     * @return the visible WebElement
     */
    public static WebElement waitForVisible(By locator, int timeoutSeconds) {
        log.debug("Waiting {}s for visible: {}", timeoutSeconds, locator);
        return getWait(timeoutSeconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits for all elements matching the locator to be visible.
     *
     * @param locator the By locator
     * @return list of visible WebElements
     */
    public static List<WebElement> waitForAllVisible(By locator) {
        log.debug("Waiting {}s for all visible: {}", DEFAULT_TIMEOUT, locator);
        return getWait(DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    /**
     * Waits for an element to be clickable (visible and enabled).
     *
     * @param locator the By locator
     * @return the clickable WebElement
     */
    public static WebElement waitForClickable(By locator) {
        return waitForClickable(locator, DEFAULT_TIMEOUT);
    }

    /**
     * Waits for an element to be clickable using a custom timeout.
     *
     * @param locator        the By locator
     * @param timeoutSeconds custom timeout in seconds
     * @return the clickable WebElement
     */
    public static WebElement waitForClickable(By locator, int timeoutSeconds) {
        log.debug("Waiting {}s for clickable: {}", timeoutSeconds, locator);
        return getWait(timeoutSeconds).until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Waits for an element to be present in the DOM.
     * The element does not need to be visible - use for hidden elements.
     *
     * @param locator the By locator
     * @return the WebElement
     */
    public static WebElement waitForPresent(By locator) {
        log.debug("Waiting {}s for present: {}", DEFAULT_TIMEOUT, locator);
        return getWait(DEFAULT_TIMEOUT).until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Waits for an element to disappear from the screen.
     * Use for loading spinners, progress bars, dialogs dismissing.
     *
     * @param locator the By locator
     * @return true when the element is no longer visible
     */
    public static boolean waitForInvisible(By locator) {
        return waitForInvisible(locator, DEFAULT_TIMEOUT);
    }

    /**
     * Waits for an element to disappear using a custom timeout.
     *
     * @param locator        the By locator
     * @param timeoutSeconds custom timeout in seconds
     * @return true when the element is no longer visible
     */
    public static boolean waitForInvisible(By locator, int timeoutSeconds) {
        log.debug("Waiting {}s for invisible: {}", timeoutSeconds, locator);
        return getWait(timeoutSeconds).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Waits for an element to contain specific text.
     *
     * @param locator the By locator
     * @param text    the text to wait for
     * @return true when the element contains the text
     */
    public static boolean waitForText(By locator, String text) {
        log.debug("Waiting for text '{}' in: {}", text, locator);
        return getWait(DEFAULT_TIMEOUT).until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    /**
     * Checks if an element is currently visible without throwing an exception.
     * Uses a short timeout - intended for conditional checks not hard waits.
     *
     * @param locator the By locator
     * @return true if visible within SHORT_TIMEOUT seconds, false otherwise
     */
    public static boolean isVisible(By locator) {
        return isVisible(locator, SHORT_TIMEOUT);
    }

    /**
     * Checks if an element is currently visible within a custom timeout.
     *
     * @param locator        the By locator
     * @param timeoutSeconds custom timeout in seconds
     * @return true if visible, false otherwise
     */
    public static boolean isVisible(By locator, int timeoutSeconds) {
        try {
            waitForVisible(locator, timeoutSeconds);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Waits for any custom condition using the default timeout.
     * Use when none of the typed methods above fit your use case.
     * <p>
     * Example:
     * WaitUtils.waitForCondition(driver -> driver.findElements(By.id("list_item")).size() > 3);
     *
     * @param condition a function that returns non-null/true when satisfied
     * @param <T>       the return type of the condition
     * @return the result of the condition
     */
    public static <T> T waitForCondition(Function<AndroidDriver, T> condition) {
        return waitForCondition(condition, DEFAULT_TIMEOUT);
    }

    /**
     * Waits for any custom condition using a custom timeout.
     *
     * @param condition      a function that returns non-null/true when satisfied
     * @param timeoutSeconds custom timeout in seconds
     * @param <T>            the return type of the condition
     * @return the result of the condition
     */
    public static <T> T waitForCondition(Function<AndroidDriver, T> condition, int timeoutSeconds) {
        log.debug("Waiting {}s for custom condition", timeoutSeconds);
        return getWait(timeoutSeconds).until(condition::apply);
    }

    /**
     * Builds a FluentWait for the current thread's driver.
     * Ignores NoSuchElementException and StaleElementReferenceException
     * which are the two most common transient exceptions during polling.
     *
     * @param timeoutSeconds how long to wait before throwing TimeoutException
     */
    private static FluentWait<AndroidDriver> getWait(int timeoutSeconds) {
        return new FluentWait<>(DriverManager.getInstance().getDriver())
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(POLLING_INTERVAL_MS))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }
}
