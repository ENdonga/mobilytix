package io.mobilytix.tests.sauce_demo;

import io.mobilytix.annotation.AppUnderTest;
import io.mobilytix.config.ConfigLoader;
import io.mobilytix.config.CredentialKeys;
import io.mobilytix.pages.sauce_demo.CatalogPage;
import io.mobilytix.pages.sauce_demo.LoginPage;
import io.mobilytix.pages.sauce_demo.MenuPage;
import io.mobilytix.tests.BaseTest;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@AppUnderTest("sauce_demo")
@Epic("Sauce Labs Demo App")
@Feature("Login")
public class LoginTest extends BaseTest {
    private LoginPage loginPage;
    private CatalogPage catalogPage;
    // Credentials resolved once for the class — from .env or CI environment
    private String validUsername;
    private String validPassword;
    private String lockedUsername;

    @BeforeClass(dependsOnMethods = "setUp")
    public void setUpPage() {
        loginPage = LoginPage.getInstance();
        catalogPage = CatalogPage.getInstance();
        ConfigLoader config = ConfigLoader.getInstance();
        validUsername = config.getCredential(CredentialKeys.SAUCE_USERNAME, "bod@example.com").trim();
        validPassword = config.getCredential(CredentialKeys.SAUCE_PASSWORD, "10203040").trim();
        lockedUsername = config.getCredential(CredentialKeys.SAUCE_LOCKED_USERNAME, "alice@example.com").trim();
        log.info("Login tests will use username: {}", validUsername);
    }

    @BeforeMethod
    public void resetToLoginScreen() {
        if (loginPage.isLoaded()) {
            log.info("Already on login screen — skipping navigation");
            loginPage.clearFields();
            return;
        }
        catalogPage.waitForPageLoad();
        catalogPage.resetAndNavigateToLogin();
        loginPage.waitForPageLoad();
    }

    @Test(description = "Valid user can log in successfully", priority = 1)
    @Story("Valid login")
    @Severity(SeverityLevel.BLOCKER)
    public void testValidLogin() {
        // Given the catalog is loaded and app state is clean
        // When the user logs in with valid credentials
        loginPage.login(validUsername, validPassword);
        catalogPage.waitForPageLoad();
        // Then the catalog is displayed confirming successful login
        Assert.assertTrue(catalogPage.isUserLoggedIn(), "Log out menu should be visible after successful login");
    }

    @Test(description = "Valid user shortcut auto-populates and logs in", priority = 2)
    @Story("Valid login")
    @Severity(SeverityLevel.NORMAL)
    public void testValidLoginWithUserShortcut() {
        // Given the user is on the login screen
        // When the user taps the valid user shortcut
        loginPage.tapValidUserShortcut();
        // Then the username field is auto-populated
        Assert.assertFalse(loginPage.getUsernameFieldText().isBlank(), "Username field should be populated after tapping shortcut");
        loginPage.tapLoginButton();
        catalogPage.waitForPageLoad();
        Assert.assertTrue(catalogPage.isUserLoggedIn(), "Log out menu should be visible after successful login");
    }

    @Test(description = "Empty username shows validation error", priority = 3)
    @Story("Field validation")
    @Severity(SeverityLevel.NORMAL)
    public void testEmptyUsernameShowsError() {
        // Given the user is on the login screen
        // When the user submits with empty username
        loginPage.enterPassword(validPassword);
        loginPage.tapLoginButton();
        // Then a validation error is shown
        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error should appear when username is empty");
    }

    @Test(description = "Empty password shows validation error", priority = 4)
    @Story("Field validation")
    @Severity(SeverityLevel.NORMAL)
    public void testEmptyPasswordShowsError() {
        // Given the user is on the login screen
        // When the user submits with empty password
        loginPage.enterUsername(validUsername);
        loginPage.tapLoginButton();
        // Then a validation error is shown
        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error should appear when password is empty");
    }

    @Test(description = "Locked user is blocked from logging in", priority = 5)
    @Story("Locked user")
    @Severity(SeverityLevel.CRITICAL)
    public void testLockedUserIsBlocked() {
        // Given the user is on the login screen
        // When the locked user attempts to log in
        loginPage.tapLockedUserShortcut();
        loginPage.tapLoginButton();
        // Then an error is shown and the catalog is not accessible
        Assert.assertTrue(loginPage.isErrorDisplayed(), "Locked user should see an error message");
        Assert.assertTrue(loginPage.isLoaded(), "Locked user should remain on login screen");
        Assert.assertFalse(catalogPage.isLoaded(), "Locked user should not reach the catalog");
    }

    @Test(description = "Logged in user can log out successfully", priority = 6)
    @Story("Logout")
    @Severity(SeverityLevel.CRITICAL)
    public void testLogoutFlow() {
        // Given a logged in user
        loginPage.login(validUsername, validPassword);
        catalogPage.waitForPageLoad();
        Assert.assertTrue(catalogPage.isLoaded(), "Should be on catalog after login");
        // When user logs out via menu
        catalogPage.tapMenu();
        MenuPage.getInstance().logout();
        // Then the login screen is shown confirming logout
        Assert.assertTrue(loginPage.isLoaded(), "Login screen should be visible after logout");
    }
}
