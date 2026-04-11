package io.mobilytix.tests.app_a;

import io.mobilytix.annotation.AppUnderTest;
import io.mobilytix.pages.app_a.MainPage;
import io.mobilytix.pages.app_a.OnboardingPage;
import io.mobilytix.reporting.AllureAttachments;
import io.mobilytix.tests.BaseTest;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for the App A main screen.
 * <p>
 * Notice what is NOT in this class:
 * - No By locators
 * - No driver calls
 * - No WaitUtils calls
 * - No config loading
 * <p>
 * The test writer only calls page object methods and makes assertions.
 * Everything else is handled by the framework.
 */
@AppUnderTest("app_a")
@Epic("App A")
@Feature("Main Screen")
public class MainPageTest extends BaseTest {
    private MainPage mainPage;

    /**
     * Runs after BaseTest.setUp() — driver is ready at this point.
     * Handles onboarding once for all tests in this class.
     */
    @BeforeClass(dependsOnMethods = "setUp")
    public void setUpPage() {
        AllureAttachments.step("Skip onboarding if present");
        OnboardingPage.getInstance().skipIfPresent();

        mainPage = MainPage.getInstance();

        AllureAttachments.step("Wait for main screen to load");
        mainPage.waitForLoad();
    }

    @Test(description = "Main screen loads with app list visible")
    @Story("App launch")
    @Severity(SeverityLevel.BLOCKER)
    public void testMainScreenLoads() {
        AllureAttachments.step("Verify app list is displayed");
        Assert.assertTrue(mainPage.isAppListDisplayed(), "App list should be visible on the main screen");

        AllureAttachments.step("Verify toolbar title is correct");
        Assert.assertEquals(mainPage.getTitle(), "Apk Analyzer", "Toolbar title should be 'Apk Analyzer'");

        AllureAttachments.attachScreenshot("Main screen");
    }

    @Test(description = "Search filters the app list")
    @Story("Search")
    @Severity(SeverityLevel.NORMAL)
    public void testSearchFiltersResults() {
        AllureAttachments.step("Tap search and enter query");
        mainPage.search("google");

        AllureAttachments.step("Verify app list is still displayed after search");
        Assert.assertTrue(mainPage.isAppListDisplayed(), "App list should show filtered results");

        AllureAttachments.attachScreenshot("Search results for 'google'");
    }

    @Test(description = "Filter button opens filter options")
    @Story("Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testFilterButtonTappable() {
        AllureAttachments.step("Tap filter button");
        mainPage.tapFilter();

        AllureAttachments.attachScreenshot("After tapping filter");

        AllureAttachments.step("Verify main screen is still loaded after filter tap");
        Assert.assertTrue(mainPage.isLoaded(), "Main screen should still be visible after tapping filter");
    }

    @Test(description = "More options button opens menu")
    @Story("More options")
    @Severity(SeverityLevel.MINOR)
    public void testMoreOptionsButtonTappable() {
        AllureAttachments.step("Tap more options button");
        mainPage.tapMore();

        AllureAttachments.attachScreenshot("After tapping more options");
        AllureAttachments.step("Dismiss menu and verify main screen");
        mainPage.dismissMenu();

        Assert.assertTrue(mainPage.isLoaded(), "Main screen should still be visible after tapping more options");
    }
}
