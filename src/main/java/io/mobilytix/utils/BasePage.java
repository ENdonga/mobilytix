package io.mobilytix.utils;

import io.appium.java_client.android.AndroidDriver;
import io.mobilytix.core.DriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;

/**
 * Base class for all page objects in the framework.
 * <p>
 * Rules for subclasses:
 * 1. Never call driver() directly in a subclass - use the protected methods here
 * 2. Never expose By locators outside the page class - keep them private
 * 3. Every page class must implement isLoaded() to verify the page is ready
 * 4. Method names should describe user actions, not technical operations
 * Good : enterUsername(), tapLoginButton(), selectCountry()
 * Bad  : findUsernameField(), clickElement(), sendKeys()
 * <p>
 * All interactions are logged at DEBUG level automatically.
 * Screenshots can be taken at any point via takeScreenshot().
 */
public abstract class BasePage {
    protected final Logger log = LogManager.getLogger(this.getClass());

    /**
     * Returns the AndroidDriver for the current thread.
     * Prefer using the protected interaction methods over calling this directly.
     */
    protected AndroidDriver driver() {
        return DriverManager.getInstance().getDriver();
    }

    /**
     * Finds and returns a visible element.
     * Waits up to the default explicit timeout.
     *
     * @param locator the By locator
     * @return the visible WebElement
     */
    protected WebElement find(By locator) {
        return WaitUtils.waitForVisible(locator);
    }

    /**
     * Finds and returns a list of all visible elements matching the locator.
     *
     * @param locator the By locator
     * @return list of visible WebElements
     */
    protected List<WebElement> findAll(By locator) {
        return WaitUtils.waitForAllVisible(locator);
    }

    /**
     * Taps an element. Waits for it to be clickable first.
     *
     * @param locator the By locator
     */
    protected void tap(By locator) {
        log.debug("Tap: {}", locator);
        WaitUtils.waitForClickable(locator).click();
    }

    /**
     * Clears and types text into an input field.
     * Waits for the field to be clickable before typing.
     *
     * @param locator the By locator
     * @param text    the text to type
     */
    protected void type(By locator, String text) {
        log.debug("Typing: '{}' into: {}", text, locator);
        WebElement field = WaitUtils.waitForClickable(locator);
        field.clear();
        field.sendKeys(text);
    }

    /**
     * Returns the trimmed visible text of an element.
     *
     * @param locator the By locator
     * @return the element's text content trimmed of whitespace
     */
    protected String getText(By locator) {
        String text = WaitUtils.waitForClickable(locator).getText().trim();
        log.debug("Getting text '{}' from: {}", text, locator);
        return text;
    }

    protected String getAttribute(By locator, String attribute) {
        return WaitUtils.waitForVisible(locator).getAttribute(attribute);
    }

    /**
     * Returns true if an element is currently checked.
     * For checkboxes, toggles, and radio buttons.
     *
     * @param locator the By locator
     * @return true if checked attribute is "true"
     */
    protected boolean isChecked(By locator) {
        return "true".equals(getAttribute(locator, "checked"));
    }

    /**
     * Returns true if an element is visible within the default short timeout.
     * Does not throw - use for conditional logic in page objects.
     *
     * @param locator the By locator
     * @return true if visible, false otherwise
     */
    protected boolean isDisplayed(By locator) {
        return WaitUtils.isVisible(locator);
    }

    /**
     * Returns true if an element is visible within a custom timeout.
     *
     * @param locator        the By locator
     * @param timeoutSeconds custom timeout in seconds
     * @return true if visible, false otherwise
     */
    protected boolean isDisplayed(By locator, int timeoutSeconds) {
        return WaitUtils.isVisible(locator, timeoutSeconds);
    }

    /**
     * Waits for an element to disappear.
     * Use for loading spinners and progress indicators.
     *
     * @param locator the By locator
     */
    protected void waitForGone(By locator) {
        log.debug("Waiting for element to disappear: {}", locator);
        WaitUtils.waitForInvisible(locator);
    }

    /**
     * Waits for an element to contain specific text.
     *
     * @param locator the By locator
     * @param text    the expected text
     */
    protected void waitForText(By locator, String text) {
        log.debug("Waiting for text '{}' in: {}", text, locator);
        WaitUtils.waitForText(locator, text);
    }

    /**
     * Scrolls down one screen length using the mobile scroll script.
     */
    protected void scrollDown() {
        log.debug("Scrolling down");
        driver().execute("mobile: scroll", Map.of("direction", "down"));
    }

    /**
     * Scrolls up one screen length using the mobile scroll script.
     */
    protected void scrollUp() {
        log.debug("Scrolling up");
        driver().execute("mobile: scroll", Map.of("direction", "up"));
    }

    /**
     * Scrolls down until the element is visible or max attempts are reached.
     * Throws RuntimeException if the element is not found after scrolling.
     *
     * @param locator     the By locator to scroll to
     * @param maxAttempts maximum number of scroll attempts
     */
    protected void scrollToElement(By locator, int maxAttempts) {
        log.debug("Scrolling to element: {} (max {} attempts)", locator, maxAttempts);
        for (int i = 0; i < maxAttempts; i++) {
            if (isDisplayed(locator, 1)) {
                log.debug("Element found after {} scroll(s)", i);
                return;
            }
            scrollDown();
        }
        throw new RuntimeException("Element not found after " + maxAttempts + " scroll attempts: " + locator);
    }

    /**
     * Scrolls down until the element is visible - uses 5 attempts by default.
     *
     * @param locator the By locator to scroll to
     */
    protected void scrollToElement(By locator) {
        scrollToElement(locator, 5);
    }

    /**
     * Takes a screenshot of the current screen.
     *
     * @return screenshot as a byte array - pass to reporting utilities
     */
    public byte[] takeScreenshot() {
        log.debug("Taking screenshot");
        return driver().getScreenshotAs(OutputType.BYTES);
    }

    /**
     * Every page object must implement this method.
     * It should check for a key element that confirms the page is fully loaded.
     * <p>
     * Called by BaseTest and page navigation methods to verify the correct screen is displayed before interacting with it.
     * <p>
     * Example implementation:
     * public boolean isLoaded() {
     * return isDisplayed(LOGIN_BUTTON);
     * }
     *
     * @return true if the page is fully loaded and ready for interaction
     */
    public abstract boolean isLoaded();
}
