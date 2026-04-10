package io.mobilytix.tests.app_a;

import io.mobilytix.annotation.AppUnderTest;
import io.mobilytix.pages.app_a.ApkAnalyzerDetailPage;
import io.mobilytix.pages.app_a.MainPage;
import io.mobilytix.pages.app_a.OnboardingPage;
import io.mobilytix.reporting.AllureAttachments;
import io.mobilytix.tests.BaseTest;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@AppUnderTest("app_a")
@Epic("Apk Analyzer")
@Feature("APK Analyzer Detail Screen")
public class ApkAnalyzerDetailPageTest extends BaseTest {
    private MainPage mainPage;
    private ApkAnalyzerDetailPage detailPage;

    private static final String TARGET_APP = "Apk Analyzer";
    private static final String TARGET_PACKAGE = "sk.styk.martin.apkanalyzer";

    @BeforeClass(alwaysRun = true, dependsOnMethods = "setUp")
    public void setUpPage() {
        AllureAttachments.step("Skip onboarding if present");
        OnboardingPage.getInstance().skipIfPresent();

        mainPage = MainPage.getInstance();
        detailPage = ApkAnalyzerDetailPage.getInstance();

        AllureAttachments.step("Wait for main screen to load");
        mainPage.waitForLoad();

        AllureAttachments.step("Search for target app");
        mainPage.search(TARGET_APP);

        AllureAttachments.step("Open app detail");
        mainPage.tapFirstResult();
    }

    @Test(description = "APK detail screen loads for selected app", priority = 1)
    @Story("Open detail")
    @Severity(SeverityLevel.BLOCKER)
    public void testDetailScreenLoads() {
        AllureAttachments.step("Verify detail screen is loaded");
        Assert.assertTrue(detailPage.isLoaded(), "APK detail screen should be visible");
        AllureAttachments.attachScreenshot("APK detail screen");
    }

    @Test(description = "App name matches searched app", priority = 2)
    @Story("App metadata")
    @Severity(SeverityLevel.CRITICAL)
    public void testAppNameIsCorrect() {
        AllureAttachments.step("Verify app name in detail header");
        String appName = detailPage.getAppName();
        log.info("App name on detail screen: {}", appName);
        Assert.assertEquals(appName, TARGET_APP, "App name should match the searched app");
    }

    @Test(description = "Package name matches expected value", priority = 3)
    @Story("App metadata")
    @Severity(SeverityLevel.CRITICAL)
    public void testPackageNameIsCorrect() {
        AllureAttachments.step("Verify package name in detail header");
        String packageName = detailPage.getPackageName();
        log.info("Package name on detail screen: {}", packageName);
        Assert.assertEquals(packageName, TARGET_PACKAGE, "Package name should match the expected value");
    }

    @Test(description = "General tab is visible and tappable", priority = 4)
    @Story("Tab navigation")
    @Severity(SeverityLevel.NORMAL)
    public void testGeneralTabTappable() {
        AllureAttachments.step("Tap General tab");
        detailPage.tapGeneralTab();
        AllureAttachments.step("Verify detail screen still loaded");
        Assert.assertTrue(detailPage.isLoaded(), "Detail screen should remain loaded after tapping General tab");
        AllureAttachments.attachScreenshot("General tab");
    }

    @Test(description = "Navigate back returns to main screen", priority = 5)
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    public void testNavigateBackToMainScreen() {
        AllureAttachments.step("Tap back button");
        detailPage.navigateBack();
        AllureAttachments.step("Verify main screen is restored");
        Assert.assertTrue(mainPage.isLoaded(), "Main screen should be visible after navigating back");
        AllureAttachments.attachScreenshot("Back on main screen");
    }
}
