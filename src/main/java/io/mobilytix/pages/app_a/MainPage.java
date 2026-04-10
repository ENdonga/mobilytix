package io.mobilytix.pages.app_a;

import io.mobilytix.utils.BasePage;
import io.mobilytix.utils.LocatorFactory;
import org.openqa.selenium.By;

public class MainPage extends BasePage {
    private static final By TOOLBAR_TITLE = LocatorFactory.byText("Apk Analyzer");
    private static final By TOOLBAR = LocatorFactory.byId("sk.styk.martin.apkanalyzer:id/toolbar");
    private static final By APP_LIST = LocatorFactory.byId("sk.styk.martin.apkanalyzer:id/recycler_view_app_list");
    private static final By SEARCH_BUTTON = LocatorFactory.byId("sk.styk.martin.apkanalyzer:id/action_search");
    private static final By SEARCH_INPUT = LocatorFactory.byId("android:id/search_src_text");
    private static final By FILTER_BUTTON = LocatorFactory.byAccessibility("Filter");
    private static final By GOOGLE_PLAY_OPTION = LocatorFactory.byText("Google Play");
    private static final By MORE_BUTTON = LocatorFactory.byUiAutomator("new UiSelector().description(\"More options\")");
    private static final By FIRST_LIST_ITEM = LocatorFactory.byId("sk.styk.martin.apkanalyzer:id/recycler_view_app_list");

    private static MainPage instance;

    private MainPage() {
    }

    public static MainPage getInstance() {
        if (instance == null) {
            instance = new MainPage();
        }
        return instance;
    }

    /**
     * Waits for the app list to be visible.
     * The list appears once the app finishes loading installed packages.
     */
    public MainPage waitForLoad() {
        log.info("Waiting for main screen to load");
        find(APP_LIST);
        return this;
    }

    /**
     * Taps the search icon and enters a search query.
     *
     * @param query the text to search for
     */
    public MainPage search(String query) {
        log.info("Searching for '{}'", query);
        tap(SEARCH_BUTTON);
        type(SEARCH_INPUT, query);
        return this;
    }

    /**
     * Taps the filter button to open filter options.
     */
    public MainPage tapFilter() {
        log.info("Tapping filter button");
        tap(FILTER_BUTTON);
        isDisplayed(GOOGLE_PLAY_OPTION);
        tap(GOOGLE_PLAY_OPTION);
        return this;
    }

    /**
     * Taps the more options button (three dot menu).
     */
    public MainPage tapMore() {
        log.info("Tapping more options button");
        tap(MORE_BUTTON);
        return this;
    }

    /**
     * Dismisses any open overlay menu by pressing the device back key.
     * Call after tapMore() or tapFilter() if a menu blocks the screen.
     */
    public MainPage dismissMenu() {
        log.info("Dismissing open menu");
        driver().navigate().back();
        return this;
    }

    /**
     * Returns the toolbar title text.
     */
    public String getTitle() {
        return getText(TOOLBAR_TITLE);
    }

    /**
     * Returns true if the app list is displayed.
     */
    public boolean isAppListDisplayed() {
        return isDisplayed(APP_LIST);
    }

    /**
     * Taps the first item in the app list.
     * Call after search() to open the top result.
     */
    public void tapFirstResult() {
        log.info("Tapping first result in list");
        // Find first item inside the recycler view
        By firstItem = LocatorFactory.byUiAutomator("new UiSelector().resourceId(\"sk.styk.martin.apkanalyzer:id/recycler_view_app_list\")" +
                ".childSelector(new UiSelector().index(0))");
        tap(firstItem);
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(APP_LIST);
    }
}
