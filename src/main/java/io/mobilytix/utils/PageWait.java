package io.mobilytix.utils;

import org.openqa.selenium.By;

public class PageWait {
    private By currentLocator;

    PageWait(By locator) {
        this.currentLocator = locator;
    }

    public PageWait toBeVisible() {
        WaitUtils.waitForVisible(currentLocator);
        return this;
    }

    public PageWait toBeClickable() {
        WaitUtils.waitForClickable(currentLocator);
        return this;
    }

    public PageWait toBeGone() {
        WaitUtils.waitForInvisible(currentLocator);
        return this;
    }

    /**
     * Switches the current locator for the next wait in the chain.
     */
    public PageWait then(By locator) {
        this.currentLocator = locator;
        return this;
    }

    /**
     * Terminates the chain. No-op — exists for readability.
     */
    public void done() {
    }
}
