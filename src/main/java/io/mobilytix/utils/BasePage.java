package io.mobilytix.utils;

import io.appium.java_client.android.AndroidDriver;
import io.mobilytix.core.DriverManager;
import io.mobilytix.exceptions.PageNotLoadedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebElement;

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

    public static final String MOBILE_SCROLL = "mobile: scroll";
    public static final String MOBILE_SCROLL_GESTURE = "mobile: scrollGesture";
    public static final String DIRECTION = "direction";
    public static final String DOWN = "down";
    public static final String UP = "up";

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

    /**
     * Waits for the page to be fully loaded and ready for interaction.
     * Every page must define what "fully loaded" means — not just visible
     * but interactive. Called before any test interaction begins.
     * <p>
     * Implement by waiting for the most stable interactive element on the screen.
     */
    public abstract void waitForPageLoad();

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
        log.info("Tap: {}", locator);
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
        log.info("Typing: '{}' into: {}", text, locator);
        performType(locator, text);
    }

    /**
     * Secure typing for passwords, CVVs, or API keys.
     * Masks the value in logs while sending the actual value to the app.
     */
    protected void typeSecret(By locator, String secretText) {
        log.info("Typing: '*******' into: {}", locator);
        performType(locator, secretText);
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
        driver().executeScript(MOBILE_SCROLL, Map.of(DIRECTION, DOWN));
    }

    protected void scrollDown(By locator) {
        log.info("Scrolling down within: {}", locator);
        try {
            driver().executeScript(MOBILE_SCROLL, Map.of(
                    "strategy", resolveScrollStrategy(locator),
                    "selector", resolveScrollSelector(locator),
                    "direction", DOWN));
        } catch (NoSuchElementException e) {
            log.debug("Reached end of scrollable container: {}", locator);
        }
    }

    /**
     * Scrolls up one screen length using the mobile scroll script.
     */
    protected void scrollUp() {
        log.debug("Scrolling up");
        driver().executeScript(MOBILE_SCROLL, Map.of(DIRECTION, UP));
    }

    protected void scrollUp(By locator) {
        log.info("Scrolling up within: {}", locator);
        WebElement element = find(locator);
        driver().executeScript(MOBILE_SCROLL_GESTURE, Map.of(
                "elementId", ((RemoteWebElement) element).getId(),
                "percent", 3.0,
                "direction", "up")
        );
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
        throw new PageNotLoadedException("Element not found after " + maxAttempts + " scroll attempts: " + locator);
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
     * Fluent wait builder for chaining visibility and clickability checks.
     * <p>
     * Usage:
     * waitFor(MENU_BUTTON).toBeVisible().toBeClickable().done();
     * waitFor(PRODUCT_LIST).toBeVisible().then(MENU_BUTTON).toBeClickable().done();
     */
    protected PageWait waitFor(By locator) {
        return new PageWait(locator);
    }

    private void performType(By locator, String text) {
        WebElement field = WaitUtils.waitForClickable(locator);
        field.clear();
        field.sendKeys(text);
    }

    /**
     * Resolves the strategy string required by mobile: scroll.
     * Supported values: "accessibility id", "class name", "-android uiautomator"
     * <p>
     * By.id()              → "-android uiautomator" (resource-id query)
     * By.accessibilityId() → "accessibility id"
     * By.className()       → "class name"
     * By.xpath()           → "-android uiautomator" (closest match)
     */
    private String resolveScrollStrategy(By locator) {
        String raw = locator.toString();
        if (raw.startsWith("By.id:") || raw.startsWith("AppiumBy.id:")) {
            return "-android uiautomator";
        }
        if (raw.startsWith("AppiumBy.accessibility id:") || raw.startsWith("By.accessibility id:")) {
            return "accessibility id";
        }
        if (raw.startsWith("By.className:")) {
            return "class name";
        }
        return "-android uiautomator";
    }

    /**
     * Resolves the selector string required by mobile: scroll.
     * For By.id locators, wraps the value in a UiAutomator resourceId query
     * since "id" is not a supported strategy for mobile: scroll.
     */
    private String resolveScrollSelector(By locator) {
        String raw = locator.toString();
        if (raw.startsWith("By.id:") || raw.startsWith("AppiumBy.id:")) {
            String prefix = raw.startsWith("By.id:") ? "By.id:" : "AppiumBy.id:";
            String resourceId = raw.substring(prefix.length()).trim();
            return "new UiSelector().resourceId(\"" + resourceId + "\")";
        }
        int separatorIndex = raw.indexOf(": ");
        if (separatorIndex == -1) {
            throw new IllegalArgumentException(
                    "Cannot extract selector from locator: " + raw);
        }
        return raw.substring(separatorIndex + 2).trim();
    }
}
