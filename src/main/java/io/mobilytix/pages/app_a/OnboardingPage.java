package io.mobilytix.pages.app_a;

import io.mobilytix.utils.BasePage;
import io.mobilytix.utils.LocatorFactory;
import org.openqa.selenium.By;

/**
 * Handles the first-launch onboarding screens.
 * These appear on fresh install or after full reset.
 * Call skipIfPresent() at the start of any test that might
 * encounter onboarding.
 */
public class OnboardingPage extends BasePage {
    private static final By SKIP_BUTTON = LocatorFactory.byId("skip");
    private static final By NEXT_BUTTON = LocatorFactory.byText("NEXT");
    private static final By GET_STARTED = LocatorFactory.byText("GET STARTED");

    private static OnboardingPage instance;

    private OnboardingPage() {
    }

    public static OnboardingPage getInstance() {
        if (instance == null) instance = new OnboardingPage();
        return instance;
    }

    /**
     * Skips onboarding if the skip button is present.
     * Safe to call even if onboarding is not showing — returns immediately.
     */
    public void skipIfPresent() {
        if (isDisplayed(SKIP_BUTTON, 3)) {
            log.info("Onboarding detected — tapping SKIP");
            tap(SKIP_BUTTON);
        } else {
            log.debug("No onboarding screen detected — skipping");
        }
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(SKIP_BUTTON, 3);
    }
}
