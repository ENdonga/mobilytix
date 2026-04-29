package io.mobilytix.pages.sauce_demo;

import io.mobilytix.utils.BasePage;
import io.mobilytix.utils.LocatorFactory;
import io.mobilytix.utils.WaitUtils;
import org.openqa.selenium.By;

/**
 * Page object for the Sauce Labs Demo App login screen.
 * <p>
 * The login screen shows a list of pre-filled usernames that auto-populate
 * the fields when tapped. Tests can use either approach:
 * - tapUsername() to auto-populate and login in one step
 * - enterUsername() + enterPassword() + tapLoginButton() for manual entry
 */
public class LoginPage extends BasePage {
    private static final By USERNAME_FIELD = LocatorFactory.byId("nameET");
    private static final By PASSWORD_FIELD = LocatorFactory.byId("passwordET");
    private static final By LOGIN_BUTTON = LocatorFactory.byAccessibility("Tap to login with given credentials");
    private static final By USERNAME_SHORTCUT_1 = LocatorFactory.byId("username1TV");
    private static final By USERNAME_SHORTCUT_2 = LocatorFactory.byId("username2TV");
    private static final By PASSWORD_ERROR_MESSAGE = LocatorFactory.byId("passwordErrorTV");
    private static final By USERNAME_ERROR_MESSAGE = LocatorFactory.byId("nameErrorTV");

    private static LoginPage instance;

    private LoginPage() {
    }

    public static LoginPage getInstance() {
        if (instance == null) {
            instance = new LoginPage();
        }
        return instance;
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(LOGIN_BUTTON);
    }

    @Override
    public void waitForPageLoad() {
        waitFor(LOGIN_BUTTON).toBeVisible().toBeClickable().done();
    }

    /**
     * Enters credentials and taps login in one call.
     *
     * @param username the email address
     * @param password the password
     */
    public void login(String username, String password) {
        log.info("Attempting login for user: {}", username);
        enterUsername(username);
        enterPassword(password);
        tapLoginButton();
    }

    /**
     * Enters a username into the username field.
     *
     * @param username the email address to enter
     * @return this page for chaining
     */
    public LoginPage enterUsername(String username) {
        type(USERNAME_FIELD, username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        typeSecret(PASSWORD_FIELD, password);
        return this;
    }

    public void clearFields() {
        log.debug("Clearing login fields");
        WaitUtils.waitForClickable(USERNAME_FIELD).clear();
        WaitUtils.waitForClickable(PASSWORD_FIELD).clear();
    }

    public void tapLoginButton() {
        tap(LOGIN_BUTTON);
    }

    /**
     * Taps the first pre-filled username shortcut (bod@example.com).
     * Auto-populates username and password fields.
     */
    public LoginPage tapValidUserShortcut() {
        tap(USERNAME_SHORTCUT_1);
        return this;
    }

    public LoginPage tapLockedUserShortcut() {
        tap(USERNAME_SHORTCUT_2);
        return this;
    }

    /**
     * Returns the error message text shown after a failed login attempt.
     * Returns empty string if no error is currently displayed.
     */
    public String getErrorMessage() {
        if (isDisplayed(PASSWORD_ERROR_MESSAGE, 1)) {
            return getText(PASSWORD_ERROR_MESSAGE);
        }
        if (isDisplayed(USERNAME_ERROR_MESSAGE, 1)) {
            return getText(USERNAME_ERROR_MESSAGE);
        }
        return "";
    }

    public boolean isErrorDisplayed() {
        return isDisplayed(PASSWORD_ERROR_MESSAGE, 1) || isDisplayed(USERNAME_ERROR_MESSAGE, 2);
    }

    public boolean isUsernameErrorDisplayed() {
        return isDisplayed(USERNAME_ERROR_MESSAGE, 1);
    }

    public boolean isPasswordErrorDisplayed() {
        return isDisplayed(PASSWORD_ERROR_MESSAGE, 1);
    }

    public String getUsernameFieldText() {
        return getAttribute(USERNAME_FIELD, "text");
    }

    public boolean isLoginButtonEnabled() {
        return find(LOGIN_BUTTON).isEnabled();
    }
}
