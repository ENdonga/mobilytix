package io.mobilytix.tests.sauce_demo;

import io.mobilytix.annotation.AppUnderTest;
import io.mobilytix.config.ConfigLoader;
import io.mobilytix.config.CredentialKeys;
import io.mobilytix.pages.sauce_demo.CatalogPage;
import io.mobilytix.pages.sauce_demo.LoginPage;
import io.mobilytix.reporting.AllureAttachments;
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
        validUsername = config.getCredential(CredentialKeys.SAUCE_VALID_USERNAME, "bod@example.com").trim();
        validPassword = config.getCredential(CredentialKeys.SAUCE_PASSWORD, "10203040").trim();
        lockedUsername = config.getCredential(CredentialKeys.SAUCE_LOCKED_USERNAME, "alice@example.com").trim();
        log.info("Login tests will use username: {}", validUsername);
    }

    /**
     * Resets app state and navigates to the login screen before each test.
     * resetAppState() clears cart and any existing session via the side menu.
     * navigateToLogin() opens the menu and taps Log In.
     */
    @BeforeMethod()
    public void navigateToLogin() {
        catalogPage.resetAppState();
        catalogPage.navigateToLogin();
        Assert.assertTrue(loginPage.isLoaded(), "Login screen should be visible before each test");
    }

    @Test(description = "Valid user can log in successfully", priority = 1)
    @Story("Valid login")
    @Severity(SeverityLevel.BLOCKER)
    public void testValidLogin() {
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

//    @Test(description = "Wrong password shows error message", priority = 3)
//    @Story("Invalid credentials")
//    @Severity(SeverityLevel.CRITICAL)
//    public void testWrongPasswordShowsError() {
//        AllureAttachments.step("Enter valid username with wrong password");
//        loginPage.login(validUsername, "wrongPassword");
//        AllureAttachments.step("Verify error message is displayed");
//        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error message should appear after wrong password");
//        AllureAttachments.attachScreenshot("Error after wrong password");
//    }
//
//    @Test(description = "Unknown username shows error message", priority = 4)
//    @Story("Invalid credentials")
//    @Severity(SeverityLevel.CRITICAL)
//    public void testUnknownUsernameShowsError() {
//        AllureAttachments.step("Enter non-existent username");
//        loginPage.login("nonexistent@example.com", validPassword);
//        AllureAttachments.step("Verify error message is displayed");
//        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error message should appear for unknown username");
//        AllureAttachments.attachScreenshot("Error after unknown username");
//    }

    @Test(description = "Empty username shows validation error", priority = 5)
    @Story("Field validation")
    @Severity(SeverityLevel.NORMAL)
    public void testEmptyUsernameShowsError() {
        AllureAttachments.step("Leave username empty and enter password only");
        loginPage.enterPassword(validPassword);
        loginPage.tapLoginButton();

        AllureAttachments.step("Verify validation error is shown");
        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error should appear when username is empty");

        AllureAttachments.attachScreenshot("Empty username validation");
    }

    @Test(description = "Empty password shows validation error", priority = 6)
    @Story("Field validation")
    @Severity(SeverityLevel.NORMAL)
    public void testEmptyPasswordShowsError() {
        AllureAttachments.step("Enter username and leave password empty");
        loginPage.enterUsername(validUsername);
        loginPage.tapLoginButton();

        AllureAttachments.step("Verify validation error is shown");
        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error should appear when password is empty");

        AllureAttachments.attachScreenshot("Empty password validation");
    }

    @Test(description = "Locked user is blocked from logging in", priority = 7)
    @Story("Locked user")
    @Severity(SeverityLevel.CRITICAL)
    public void testLockedUserIsBlocked() {
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
}
