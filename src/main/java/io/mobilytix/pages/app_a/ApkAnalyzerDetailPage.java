package io.mobilytix.pages.app_a;

import io.mobilytix.utils.BasePage;
import io.mobilytix.utils.LocatorFactory;
import org.openqa.selenium.By;

/**
 * Page object for the app detail screen.
 * Shown after tapping an app entry in the main list.
 */
public class ApkAnalyzerDetailPage extends BasePage {
    private static final By APP_NAME_HEADER = LocatorFactory.byId("app_package_name");
    private static final By APPLICATION_NAME_VALUE = LocatorFactory.byText("Apk Analyzer");
    private static final By VERSION_NAME = LocatorFactory.byText("3.4.0");
    private static final By BACK_BUTTON = LocatorFactory.byUiAutomator("new UiSelector().className(\"android.widget.ImageButton\")");
    // Tab labels
    private static final By GENERAL_TAB = LocatorFactory.byText("GENERAL");
    private static final By CERTIFICATE_TAB = LocatorFactory.byText("CERTIFICATE");
    private static final By PERMISSIONS_TAB = LocatorFactory.byText("USED PERMISSIONS");
    private static final By ACTIVITIES_TAB = LocatorFactory.byText("ACTIVITIES");

    private static ApkAnalyzerDetailPage instance;

    private ApkAnalyzerDetailPage() {
    }

    public static ApkAnalyzerDetailPage getInstance() {
        if (instance == null) {
            instance = new ApkAnalyzerDetailPage();
        }
        return instance;
    }

    public String getAppName() {
        return getText(APPLICATION_NAME_VALUE);
    }

    public String getPackageName() {
        return getText(APP_NAME_HEADER);
    }

    public String getVersionName() {
        return getText(VERSION_NAME);
    }

    /**
     * Taps the General tab.
     */
    public ApkAnalyzerDetailPage tapGeneralTab() {
        tap(GENERAL_TAB);
        return this;
    }

    /**
     * Taps the Certificate tab.
     */
    public ApkAnalyzerDetailPage tapCertificateTab() {
        tap(CERTIFICATE_TAB);
        return this;
    }

    /**
     * Taps the Used Permissions tab.
     */
    public ApkAnalyzerDetailPage tapPermissionsTab() {
        tap(PERMISSIONS_TAB);
        return this;
    }

    /**
     * Taps the Activities tab.
     */
    public ApkAnalyzerDetailPage tapActivitiesTab() {
        tap(ACTIVITIES_TAB);
        return this;
    }

    public void navigateBack() {
        log.info("Navigating back from detail screen");
        tap(BACK_BUTTON);
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(APP_NAME_HEADER);
    }
}
