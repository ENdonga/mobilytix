package io.mobilytix.pages.sauce_demo;

import io.mobilytix.utils.BasePage;
import io.mobilytix.utils.LocatorFactory;
import io.mobilytix.utils.WaitUtils;
import org.openqa.selenium.By;


/**
 * Page object for the Sauce Labs Demo App side navigation drawer.
 * <p>
 * The drawer is opened by tapping the hamburger menu icon in the header.
 * All menu items share the same resource-id (itemTV) so they are located
 * by content-desc where available, falling back to text.
 * <p>
 * Menu items visible in XML:
 * Catalog, WebView, QR Code Scanner, Geo Location, Drawing,
 * About, Reset App State, FingerPrint, Virtual USB,
 * Crash app (debug), Log In
 */
public class MenuPage extends BasePage {
    private static final By DRAWER_MENU = LocatorFactory.byAccessibility("View menu");
    private static final By MENU_LOGIN = LocatorFactory.byAccessibility("Login Menu Item");
    private static final By MENU_LOGOUT = LocatorFactory.byAccessibility("Logout Menu Item");
    private static final By MENU_CATALOG = LocatorFactory.byText("Catalog");
    private static final By MENU_WEB_VIEW = LocatorFactory.byText("WebView");
    private static final By MENU_RESET_APP_STATE = LocatorFactory.byText("Reset App State");
    private static final By RESET_APP_DIALOG_TITLE = LocatorFactory.byText("Reset App State");
    private static final By DIALOG_RESET_BUTTON = LocatorFactory.byText("RESET APP");
    private static final By DIALOG_CANCEL_BUTTON = LocatorFactory.byText("CANCEL");
    private static final By DIALOG_RESET_SUCCESS_MESSAGE = LocatorFactory.byText("App State has been reset.");
    private static final By DIALOG_OK_BUTTON = LocatorFactory.byText("OK");
    private static final By LOGOUT_DIALOG_TITLE = LocatorFactory.byText("Log Out");
    private static final By LOGOUT_DIALOG_CONFIRM = LocatorFactory.byText("LOGOUT");

    private static MenuPage instance;

    private MenuPage() {
    }

    public static MenuPage getInstance() {
        if (instance == null) {
            instance = new MenuPage();
        }
        return instance;
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(DRAWER_MENU);
    }

    /**
     * Navigates to the login screen from the menu.
     * Handles two states:
     * - Logged in  → logs out first, then taps Log In
     * - Logged out → taps Log In directly
     * <p>
     * Precondition: menu drawer must already be open.
     */
    public void navigateToLoginScreen() {
        if (isDisplayed(MENU_LOGOUT, 1)) {
            log.info("User is logged in — logging out first");
            logout();
            if (LoginPage.getInstance().isLoaded()) {
                return;
            }
        }
        tap(MENU_LOGIN);
    }

    /**
     * Logs the user out via the menu.
     * Handles the confirmation dialog that appears after tapping Log Out.
     * After logout the drawer closes automatically.
     * <p>
     * Precondition: menu drawer must already be open.
     */
    public void logout() {
        tap(MENU_LOGOUT);
        waitForLogoutDialog();
        tap(LOGOUT_DIALOG_CONFIRM);
    }

    public void waitForMenuToOpen() {
        WaitUtils.waitForVisible(MENU_CATALOG);
    }

    /**
     * Waits for the logout confirmation dialog to appear.
     */
    public void waitForLogoutDialog() {
        WaitUtils.waitForVisible(LOGOUT_DIALOG_TITLE);
        log.debug("Logout confirmation dialog is visible");
    }

    public void tapCatalog() {
        tap(MENU_CATALOG);
    }

    public void tapWebView() {
        tap(MENU_WEB_VIEW);
    }

    public void tapResetAppState() {
        tap(MENU_RESET_APP_STATE);
    }

    public void resetAppStateAndConfirm() {
        tap(MENU_RESET_APP_STATE);
        log.debug("Confirming reset dialog");
        tap(DIALOG_RESET_BUTTON);
        log.debug("Dismissing success notification");
        waitForResetSuccess();
        tap(DIALOG_OK_BUTTON);
    }

    public void resetAppStateAndCancel() {
        tap(MENU_RESET_APP_STATE);
        cancelResetDialog();
    }

    public void confirmResetDialog() {
        tap(DIALOG_RESET_BUTTON);
    }

    public void cancelResetDialog() {
        tap(DIALOG_CANCEL_BUTTON);
    }

    public boolean isResetDialogDisplayed() {
        return isDisplayed(RESET_APP_DIALOG_TITLE, 1) || isDisplayed(DIALOG_RESET_SUCCESS_MESSAGE, 1);
    }

    private void waitForResetSuccess() {
        WaitUtils.waitForVisible(DIALOG_RESET_SUCCESS_MESSAGE);
        log.debug("Reset success message visible: {}", getText(DIALOG_RESET_SUCCESS_MESSAGE));
    }
}
