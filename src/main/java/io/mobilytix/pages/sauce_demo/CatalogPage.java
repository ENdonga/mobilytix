package io.mobilytix.pages.sauce_demo;

import io.mobilytix.utils.BasePage;
import io.mobilytix.utils.LocatorFactory;
import org.openqa.selenium.By;


/**
 * Page object for the Sauce Labs Demo App catalog screen.
 * This is the landing screen after a successful login.
 * Also accessible without login for browsing.
 */
public class CatalogPage extends BasePage {
    private static final By PRODUCT_LIST = LocatorFactory.byId("com.saucelabs.mydemoapp.android:id/productRV");
    private static final By MENU_BUTTON = LocatorFactory.byAccessibility("View menu");
    private static final By CART_BUTTON = LocatorFactory.byAccessibility("View cart");

    private static CatalogPage instance;

    private CatalogPage() {
    }

    public static CatalogPage getInstance() {
        if (instance == null) {
            instance = new CatalogPage();
        }
        return instance;
    }

    @Override
    public boolean isLoaded() {
        return isProductListDisplayed();
    }

    /**
     * Returns true if the product list is visible.
     * Used to verify successful navigation to catalog after login.
     */
    public boolean isProductListDisplayed() {
        return isDisplayed(PRODUCT_LIST);
    }

    public CatalogPage tapMenu() {
        log.info("Opening side menu");
        tap(MENU_BUTTON);
        return this;
    }

    public void tapCart() {
        log.info("Tapping cart...");
        tap(CART_BUTTON);
    }

    public void navigateToLogin() {
        log.info("Navigating to login page via menu");
        tapMenu();
        MenuPage.getInstance().navigateToLoginScreen();
    }

    public void resetAppState() {
        log.info("Resetting app state via menu");
        tapMenu();
        MenuPage.getInstance().resetAppStateAndConfirm();
    }
}
