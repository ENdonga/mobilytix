package io.mobilytix.utils;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * Factory for building element locators.
 * <p>
 * Provides a consistent, readable API for all locator strategies used in the framework.
 * Page objects use this class instead of constructing By or AppiumBy instances directly.
 * <p>
 * Locator strategy priority (most stable to least stable):
 * 1. byId()              — resource-id, fastest and most stable
 * 2. byAccessibility()   — content-desc, good for elements without IDs
 * 3. byText()            — UiAutomator text match, robust for dynamic content
 * 4. byTextContains()    — UiAutomator partial text match
 * 5. byClass()           — class name, use when others are unavailable
 * 6. byXpath()           — last resort, fragile and slow
 * 7. byUiAutomator()     — raw UiAutomator for complex custom selectors
 * <p>
 * Usage in page objects:
 * private static final By TITLE    = LocatorFactory.byId("com.example:id/title");
 * private static final By LABEL    = LocatorFactory.byText("Process name");
 * private static final By BACK_BTN = LocatorFactory.byAccessibility("Navigate up");
 */
public class LocatorFactory {
    private static final String UI_SELECTOR_TEXT = "new UiSelector().text(\"%s\")";
    private static final String UI_SELECTOR_TEXT_CONTAINS = "new UiSelector().textContains(\"%s\")";
    private static final String UI_SELECTOR_TEXT_STARTS = "new UiSelector().textStartsWith(\"%s\")";
    private static final String UI_SELECTOR_DESCRIPTION = "new UiSelector().description(\"%s\")";
    private static final String UI_SELECTOR_CLASS = "new UiSelector().className(\"%s\")";
    private static final String UI_SELECTOR_RESOURCE_ID = "new UiSelector().resourceId(\"%s\")";
    private static final String UI_SELECTOR_INDEX = "new UiSelector().className(\"%s\").instance(%d)";
    private static final String UI_SELECTOR_SCROLLABLE = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text(\"%s\"))";

    private LocatorFactory() {
    }

    /**
     * Locates by resource-id.
     * Most stable strategy — use this first whenever an element has an ID.
     * <p>
     * Example:
     * LocatorFactory.byId("sk.styk.martin.apkanalyzer:id/app_name")
     *
     * @param resourceId the full resource-id e.g. "com.example:id/button"
     */
    public static By byId(String resourceId) {
        return By.id(resourceId);
    }

    /**
     * Locates by accessibility id (content-desc attribute).
     * Use when an element has no resource-id but has a content-desc.
     * Common for toolbar buttons, FABs, and icon buttons.
     * <p>
     * Example:
     * LocatorFactory.byAccessibility("Navigate up")
     * LocatorFactory.byAccessibility("Search")
     *
     * @param contentDesc the content-desc value visible in Appium Inspector
     */
    public static By byAccessibility(String contentDesc) {
        return AppiumBy.accessibilityId(contentDesc);
    }

    /**
     * Locates by exact visible text using UiAutomator UiSelector.
     * More robust than XPath for text-based matching.
     * Use for labels, headings, and static text elements.
     * <p>
     * Example:
     * LocatorFactory.byText("Process name")
     * LocatorFactory.byText("Application name")
     *
     * @param text the exact visible text of the element
     */
    public static By byText(String text) {
        return AppiumBy.androidUIAutomator(String.format(UI_SELECTOR_TEXT, text));
    }

    /**
     * Locates by partial visible text using UiAutomator UiSelector.
     * Use when the full text is dynamic or too long to match exactly.
     * <p>
     * Example:
     * LocatorFactory.byTextContains("version")
     * LocatorFactory.byTextContains("Android 14")
     *
     * @param partialText substring of the visible text to match
     */
    public static By byTextContains(String partialText) {
        return AppiumBy.androidUIAutomator(String.format(UI_SELECTOR_TEXT_CONTAINS, partialText));
    }

    /**
     * Locates by text that starts with the given prefix.
     * <p>
     * Example:
     * LocatorFactory.byTextStartsWith("Version")
     *
     * @param prefix the text prefix to match
     */
    public static By byTextStartsWith(String prefix) {
        return AppiumBy.androidUIAutomator(String.format(UI_SELECTOR_TEXT_STARTS, prefix));
    }

    /**
     * Locates by content description using UiAutomator.
     * Similar to byAccessibility() but uses UiAutomator under the hood
     * which gives more flexibility for complex selectors.
     *
     * @param description the content description of the element
     */
    public static By byDescription(String description) {
        return AppiumBy.androidUIAutomator(String.format(UI_SELECTOR_DESCRIPTION, description));
    }

    /**
     * Locates by Android class name.
     * Use when elements have no ID, text, or content-desc.
     * Combine with index when multiple elements share the same class.
     * <p>
     * Example:
     * LocatorFactory.byClass("android.widget.TextView")
     * LocatorFactory.byClass("android.widget.EditText")
     *
     * @param className the Android class name e.g. "android.widget.Button"
     */
    public static By byClass(String className) {
        return By.className(className);
    }

    /**
     * Locates by class name at a specific index position.
     * Use when there are multiple elements of the same class and you
     * need to target a specific one by its position (0-based).
     * <p>
     * Example:
     * LocatorFactory.byClassAtIndex("android.widget.TextView", 2)
     * → third TextView on the screen
     *
     * @param className the Android class name
     * @param index     zero-based position index
     */
    public static By byClassAtIndex(String className, int index) {
        return AppiumBy.androidUIAutomator(String.format(UI_SELECTOR_INDEX, className, index));
    }

    /**
     * Locates by XPath expression.
     * Use only when no other strategy works.
     * XPath is slow and brittle — it breaks when the UI hierarchy changes.
     * <p>
     * Example:
     * LocatorFactory.byXpath("//android.widget.TextView[@text='Hello']")
     * LocatorFactory.byXpath("//android.widget.Button[contains(@text,'Submit')]")
     *
     * @param xpath the XPath expression
     */
    public static By byXpath(String xpath) {
        return By.xpath(xpath);
    }

    /**
     * Locates using a raw UiAutomator selector string.
     * Use for complex selectors that cannot be expressed with the
     * typed methods above.
     * <p>
     * This is what Appium Inspector shows in the "android uiautomator" field.
     * Copy the selector from Inspector and pass it directly here.
     * <p>
     * Example from Appium Inspector:
     * new UiSelector().text("Process name")
     * new UiSelector().resourceId("com.example:id/btn").instance(0)
     *
     * @param uiAutomatorSelector the raw UiSelector or UiScrollable expression
     */
    public static By byUiAutomator(String uiAutomatorSelector) {
        return AppiumBy.androidUIAutomator(uiAutomatorSelector);
    }

    /**
     * Scrolls the first scrollable view until an element with the given
     * text is visible, then returns a locator for that element.
     * <p>
     * Use for elements that are off-screen in a scrollable list.
     * This is a UiScrollable selector — it both scrolls AND locates.
     * <p>
     * Example:
     * driver.findElement(LocatorFactory.scrollToText("Settings"));
     *
     * @param text the exact visible text of the element to scroll to
     */
    public static By scrollToText(String text) {
        return AppiumBy.androidUIAutomator(String.format(UI_SELECTOR_SCROLLABLE, text));
    }
}

