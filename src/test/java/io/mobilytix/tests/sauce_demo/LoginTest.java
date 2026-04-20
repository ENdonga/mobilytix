package io.mobilytix.tests.sauce_demo;

import io.mobilytix.annotation.AppUnderTest;
import io.mobilytix.config.ConfigLoader;
import io.mobilytix.config.CredentialKeys;
import io.mobilytix.pages.sauce_demo.CatalogPage;
import io.mobilytix.pages.sauce_demo.LoginPage;
import io.mobilytix.pages.sauce_demo.MenuPage;
import io.mobilytix.reporting.AllureAttachments;
import io.mobilytix.tests.BaseTest;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
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
        validUsername = config.getCredential(CredentialKeys.SAUCE_VALID_USERNAME, "bod@example.com").trim();
        validPassword = config.getCredential(CredentialKeys.SAUCE_PASSWORD, "10203040").trim();
        lockedUsername = config.getCredential(CredentialKeys.SAUCE_LOCKED_USERNAME, "alice@example.com").trim();
        log.info("Login tests will use username: {}", validUsername);
    }

    @Test(description = "Valid user can log in successfully", priority = 1)
    @Story("Valid login")
    @Severity(SeverityLevel.BLOCKER)
    public void testValidLogin() {
        AllureAttachments.step("Navigate to login with app reset");
        catalogPage.resetAndNavigateToLogin();
        AllureAttachments.step("Enter valid credentials");
        loginPage.login(validUsername, validPassword);
        AllureAttachments.step("Verify catalog screen is displayed after login");
        Assert.assertTrue(catalogPage.isLoaded(), "Catalog should be visible after successful login");
        AllureAttachments.attachScreenshot("After valid login");
    }

    @Test(description = "Valid user shortcut auto-populates and logs in", priority = 2)
    @Story("Valid login")
    @Severity(SeverityLevel.NORMAL)
    public void testValidUserShortcut() {
        AllureAttachments.step("Navigate to login with app reset");
        catalogPage.navigateToLogin();
        AllureAttachments.step("Tap valid user shortcut");
        loginPage.tapValidUserShortcut();
        AllureAttachments.step("Verify username field is populated");
        Assert.assertFalse(loginPage.getUsernameFieldText().isBlank(), "Username field should be populated after tapping shortcut");
        AllureAttachments.step("Tap login button");
        loginPage.tapLoginButton();
        AllureAttachments.step("Verify catalog screen is displayed");
        Assert.assertTrue(catalogPage.isLoaded(), "Catalog should be visible after shortcut login");
        AllureAttachments.attachScreenshot("After shortcut login");
    }

    @Test(description = "Empty username shows validation error", priority = 3)
    @Story("Field validation")
    @Severity(SeverityLevel.NORMAL)
    public void testEmptyUsernameShowsError() {
        AllureAttachments.step("Navigate to login without reset");
        catalogPage.navigateToLogin();
        AllureAttachments.step("Leave username empty and enter password only");
        loginPage.enterPassword(validPassword);
        loginPage.tapLoginButton();

        AllureAttachments.step("Verify validation error is shown");
        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error should appear when username is empty");

        AllureAttachments.attachScreenshot("Empty username validation");
    }

    @Test(description = "Empty password shows validation error", priority = 4)
    @Story("Field validation")
    @Severity(SeverityLevel.NORMAL)
    public void testEmptyPasswordShowsError() {
        AllureAttachments.step("Navigate to login without reset");
        catalogPage.navigateToLogin();
        AllureAttachments.step("Enter username and leave password empty");
        loginPage.enterUsername(validUsername);
        loginPage.tapLoginButton();

        AllureAttachments.step("Verify validation error is shown");
        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error should appear when password is empty");

        AllureAttachments.attachScreenshot("Empty password validation");
    }

    @Test(description = "Locked user is blocked from logging in", priority = 5)
    @Story("Locked user")
    @Severity(SeverityLevel.CRITICAL)
    public void testLockedUserIsBlocked() {
        AllureAttachments.step("Navigate to login without reset");
        catalogPage.navigateToLogin();
        AllureAttachments.step("Tap locked user shortcut");
        loginPage.tapLockedUserShortcut();

        AllureAttachments.step("Enter password and attempt login");
        loginPage.enterPassword(validPassword);
        loginPage.tapLoginButton();

        AllureAttachments.step("Verify error is shown for locked account");
        Assert.assertTrue(loginPage.isErrorDisplayed(), "Locked user should see an error message");

        AllureAttachments.step("Verify catalog is not accessible");
        Assert.assertFalse(catalogPage.isLoaded(), "Locked user should not reach the catalog");

        AllureAttachments.attachScreenshot("Locked user blocked");
        log.info("Locked user correctly blocked. Error: {}", loginPage.getErrorMessage());
    }

    @Test(description = "Logged in user can log out successfully", priority = 6)
    @Story("Logout")
    @Severity(SeverityLevel.CRITICAL)
    public void testLogoutFlow() {
        AllureAttachments.step("Navigate to login with reset and log in");
        catalogPage.resetAndNavigateToLogin();
        loginPage.login(validUsername, validPassword);

        AllureAttachments.step("Verify logged in — catalog visible");
        Assert.assertTrue(catalogPage.isLoaded(), "Should be on catalog after login");

        AllureAttachments.step("Open menu and tap Log Out");
        catalogPage.tapMenu();
        MenuPage.getInstance().logout();

        AllureAttachments.step("Verify returned to catalog as guest");
        Assert.assertTrue(loginPage.isLoaded(), "Should be back on login screen after logout");

        AllureAttachments.attachScreenshot("After logout");
    }
}
