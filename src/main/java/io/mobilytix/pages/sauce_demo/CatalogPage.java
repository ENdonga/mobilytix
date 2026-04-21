package io.mobilytix.pages.sauce_demo;

import io.mobilytix.utils.BasePage;
import io.mobilytix.utils.LocatorFactory;
import io.mobilytix.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Page object for the Sauce Labs Demo App catalog screen.
 * This is the landing screen after a successful login.
 * Also accessible without login for browsing.
 */
public class CatalogPage extends BasePage {
    private static final By PRODUCT_LIST = LocatorFactory.byId("com.saucelabs.mydemoapp.android:id/productRV");
    private static final By MENU_BUTTON = LocatorFactory.byAccessibility("View menu");
    private static final By CART_BUTTON = LocatorFactory.byAccessibility("View cart");
    private static final By SORT_BUTTON = LocatorFactory.byId("com.saucelabs.mydemoapp.android:id/sortIV");
    private static final By PRODUCT_TITLE = LocatorFactory.byAccessibility("Product Title");
    private static final By PRODUCT_PRICE = LocatorFactory.byAccessibility("Product Price");
    private static final By SCROLL_VIEW = LocatorFactory.byId("com.saucelabs.mydemoapp.android:id/scrollView");
    private static final By FOOTER_SECTION = LocatorFactory.byId("com.saucelabs.mydemoapp.android:id/socialLL");

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

    // Navigation
    public CatalogPage tapMenu() {
        log.info("Opening side menu");
        tap(MENU_BUTTON);
        return this;
    }

    public void tapCart() {
        log.info("Tapping cart...");
        tap(CART_BUTTON);
    }

    public void tapSortButton() {
        log.info("Opening sort dialog..");
        tap(SORT_BUTTON);
    }

    /**
     * Strategy 1 - Navigate to login WITH app state reset.
     * Use when you need a guaranteed clean state (cart cleared, session gone).
     * Adds extra steps (reset dialog) but ensures full isolation.
     * <p>
     * Flow: open menu → reset app state (2 dialogs) → open menu → navigate to login
     */
    public void resetAndNavigateToLogin() {
        log.info("Strategy: reset app state then navigate to login");
        resetAppState();
        navigateToLogin();
    }

    /**
     * Strategy 2 - Navigate to login WITHOUT app state reset.
     * Use when you only need to be on the login screen regardless of cart state.
     * Fewer steps - handles logged in/out state via logout if needed.
     * <p>
     * Flow: open menu → logout if logged in → tap Log In
     */
    public void navigateToLogin() {
        log.info("Strategy: navigate to login (no reset)");
        tapMenu();
        MenuPage.getInstance().navigateToLoginScreen();
    }

    public void resetAppState() {
        log.info("Resetting app state via menu");
        tapMenu();
        MenuPage.getInstance().resetAppStateAndConfirm();
    }

    // Product data - reads currently visible items only
    public boolean isProductListDisplayed() {
        return isDisplayed(PRODUCT_LIST);
    }

    public List<String> getVisibleProductNames() {
        List<String> names = new ArrayList<>();
        for (WebElement el : findAll(PRODUCT_TITLE)) {
            names.add(el.getText().trim());
        }
        log.debug("Visible product names: {}", names);
        return names;
    }

    public List<Double> getVisibleProductPrices() {
        List<Double> prices = new ArrayList<>();
        for (WebElement el : findAll(PRODUCT_PRICE)) {
            String rawPrice = el.getText().trim().replace("$ ", "");
            prices.add(Double.valueOf(rawPrice));
        }
        log.debug("Visible product prices: {}", prices);
        return prices;
    }

    public String getFirstProductName() {
        return findAll(PRODUCT_TITLE).get(0).getText().trim();
    }

    public Double getFirstProductPrice() {
        String raw = findAll(PRODUCT_PRICE).get(0).getText().trim().replace("$ ", "");
        return Double.valueOf(raw);
    }

    // Scrolling the product list recycler view
    public void scrollToBottom() {
        log.info("Scrolling to bottom of the catalog page");
        int maxAttempts = 10;
        int attempts = 0;
        while (!isFooterVisible() && attempts < maxAttempts) {
            scrollDown(SCROLL_VIEW);
            attempts++;
        }
        if (!isFooterVisible()) {
            log.warn("Footer not visible after {} scroll attempts", maxAttempts);
        }
    }

    public void scrollToTop() {
        log.info("Scrolling to top of catalog...");
        String lastSeenFirstItem = null;

        while (true) {
            List<String> currentNames = getVisibleProductNames();
            if (currentNames.isEmpty()) {
                scrollUp(SCROLL_VIEW);
                continue;
            }
            String currentFirstItem = currentNames.get(0);
            if (currentFirstItem.equals(lastSeenFirstItem)) {
                log.info("Reached top of catalog at: {}", currentFirstItem);
                return;
            }
            lastSeenFirstItem = currentFirstItem;
            scrollUp(SCROLL_VIEW);
            WaitUtils.waitForVisible(PRODUCT_LIST);
        }
    }

    public boolean isFooterVisible() {
        return isDisplayed(FOOTER_SECTION, 3);
    }
}
