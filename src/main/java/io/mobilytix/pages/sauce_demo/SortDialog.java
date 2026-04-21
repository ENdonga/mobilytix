package io.mobilytix.pages.sauce_demo;

import io.mobilytix.utils.BasePage;
import io.mobilytix.utils.LocatorFactory;
import io.mobilytix.utils.WaitUtils;
import org.openqa.selenium.By;

public class SortDialog extends BasePage {
    private static final By DIALOG_TITLE = LocatorFactory.byText("Sort by:");
    private static final By SORT_NAME_ASCENDING = LocatorFactory.byAccessibility("Ascending order by name");
    private static final By SORT_NAME_DESCENDING = LocatorFactory.byAccessibility("Descending order by name");
    private static final By SORT_PRICE_ASCENDING = LocatorFactory.byAccessibility("Ascending order by price");
    private static final By SORT_PRICE_DESCENDING = LocatorFactory.byAccessibility("Descending order by price");
    private static final By SELECTED_TICK = LocatorFactory.byId("com.saucelabs.mydemoapp.android:id/tickNameAscIV");

    private static SortDialog instance;

    private SortDialog() {
    }

    public static SortDialog getInstance() {
        if (instance == null) {
            instance = new SortDialog();
        }
        return instance;
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(DIALOG_TITLE, 2);
    }

    public void selectNameAscending() {
        log.info("Selecting sort: Name - Ascending");
        waitForDialogToOpen();
        tap(SORT_NAME_ASCENDING);
    }

    public void selectNameDescending() {
        log.info("Selecting sort: Name - Descending");
        waitForDialogToOpen();
        tap(SORT_NAME_DESCENDING);
    }

    public void selectPriceAscending() {
        log.info("Selecting sort: Price - Ascending");
        waitForDialogToOpen();
        tap(SORT_PRICE_ASCENDING);
    }

    public void selectPriceDescending() {
        log.info("Selecting sort: Price - Descending");
        waitForDialogToOpen();
        tap(SORT_PRICE_DESCENDING);
    }

    private void waitForDialogToOpen() {
        WaitUtils.waitForVisible(DIALOG_TITLE);
    }
}
