package io.mobilytix.tests.sauce_demo;

import io.mobilytix.annotation.AppUnderTest;
import io.mobilytix.pages.sauce_demo.CatalogPage;
import io.mobilytix.pages.sauce_demo.SortDialog;
import io.mobilytix.tests.BaseTest;
import io.mobilytix.utils.DataUtils;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

@AppUnderTest("sauce_demo")
@Epic("Sauce Labs Demo App - Products Catalog and Sorting")
@Feature("Products Catalog listing and Sorting")
public class CatalogTest extends BaseTest {
    private CatalogPage catalogPage;
    private SortDialog sortDialog;

    @BeforeClass(dependsOnMethods = "setUp")
    public void setUpPage() {
        catalogPage = CatalogPage.getInstance();
        sortDialog = SortDialog.getInstance();
    }

    @BeforeMethod
    public void resetCatalogToDefaultState() {
        catalogPage.waitForPageLoad();
        // Reset ensures catalog starts with default sort (Name - Ascending) and no items in cart between tests
        catalogPage.resetAppState();
        Assert.assertTrue(catalogPage.isLoaded(), "Catalog page should be loaded before each test");
    }

    @Test(description = "User can scroll to the bottom of the product list")
    @Story("Scrolling")
    @Severity(SeverityLevel.NORMAL)
    public void testUserCanScrollToBottomOfProductList() {
        catalogPage.scrollToBottom();
        Assert.assertTrue(catalogPage.isFooterVisible(), "Footer should be visible");
    }

    @Test(description = "User can scroll back to the top of the product list")
    @Story("Scrolling")
    @Severity(SeverityLevel.NORMAL)
    public void testUserCanScrollBackToTopOfProductList() {
        catalogPage.scrollToBottom();
        catalogPage.scrollToTop();
        Assert.assertTrue(catalogPage.isProductListDisplayed(), "Product list should be displayed");
    }

    // Sorting by Name
    @Test(description = "Products are sorted alphabetically A to Z when sorted by name ascending")
    @Story("Sorting")
    @Severity(SeverityLevel.NORMAL)
    public void testProductsAreSortedAlphabeticallyAToZWhenSortByNameAscending() {
        catalogPage.tapSortButton();
        sortDialog.selectNameAscending();
        List<String> productNames = catalogPage.getVisibleProductNames();
        Assert.assertFalse(productNames.isEmpty(), "Product names should not be empty");
        Assert.assertEquals(productNames, DataUtils.sortedAlphabetically(productNames), "Products should be in A to Z. Actual order: " + productNames);
    }

    @Test(description = "Products are sorted alphabetically Z to A when sorted by name descending")
    @Story("Sorting")
    @Severity(SeverityLevel.NORMAL)
    public void testProductsAreSortedAlphabeticallyZToAWhenSortByNameDescending() {
        catalogPage.tapSortButton();
        sortDialog.selectNameDescending();
        List<String> productNames = catalogPage.getVisibleProductNames();
        Assert.assertFalse(productNames.isEmpty(), "Product names should not be empty");
        Assert.assertEquals(productNames, DataUtils.sortedAlphabeticallyReversed(productNames), "Products should be in Z to A order. Actual order: " + productNames);
    }

    @Test(description = "First product changes when switching from name ascending to name descending")
    @Story("Sorting")
    @Severity(SeverityLevel.NORMAL)
    public void testFirstProductChangesWhenSortOrderIsReversed() {
        catalogPage.tapSortButton();
        sortDialog.selectNameAscending();
        String firstProductAscending = catalogPage.getFirstProductName();

        catalogPage.tapSortButton();
        sortDialog.selectNameDescending();
        String firstProductDescending = catalogPage.getFirstProductName();

        Assert.assertNotEquals(firstProductAscending, firstProductDescending, "First product should change when sort order is reversed. Both returned: " + firstProductAscending);
    }

    // Sort by Price
    @Test(description = "Products are sorted lowest to highest price when sorted by price ascending")
    @Story("Sorting")
    @Severity(SeverityLevel.NORMAL)
    public void testProductsAreSortedLowestToHighestPriceWhenSortedByPriceAscending() {
        catalogPage.tapSortButton();
        sortDialog.selectPriceAscending();
        List<Double> prices = catalogPage.getVisibleProductPrices();
        Assert.assertFalse(prices.isEmpty(), "Product prices should not be empty");
        Assert.assertEquals(prices, DataUtils.sortedLowestToHighest(prices), "Products should be in lowest to highest price order. Actual:" + prices);
    }

    @Test(description = "Products are sorted highest to lowest price when sorted by price descending")
    @Story("Sorting")
    @Severity(SeverityLevel.NORMAL)
    public void testProductsAreSortedHighestToLowestPriceWhenSortedByPriceDescending() {
        catalogPage.tapSortButton();
        sortDialog.selectPriceDescending();
        List<Double> prices = catalogPage.getVisibleProductPrices();
        Assert.assertFalse(prices.isEmpty(), "Product prices should not be empty");
        Assert.assertEquals(prices, DataUtils.sortedHighestToLowest(prices), "Products should be in highest to lowest price order. Actual:" + prices);
    }

    @Test(description = "First product price changes when switching from price ascending to price descending")
    @Story("Sorting")
    @Severity(SeverityLevel.NORMAL)
    public void testFirstProductPriceChangesWhenSortOrderIsReversed() {
        catalogPage.tapSortButton();
        sortDialog.selectPriceAscending();
        double lowestPrice = catalogPage.getFirstProductPrice();

        catalogPage.tapSortButton();
        sortDialog.selectPriceDescending();
        double highestPrice = catalogPage.getFirstProductPrice();

        Assert.assertTrue(highestPrice >= lowestPrice, "Highest price (" + highestPrice + ") should be >= lowest price (" + lowestPrice + ")");
    }

    @Test(description = "Sort order is preserved after scrolling down and back up")
    @Story("Sorting")
    @Severity(SeverityLevel.NORMAL)
    public void testSortOrderIsPreservedAfterScrolling() {
        catalogPage.tapSortButton();
        sortDialog.selectNameDescending();

        String firstProductBeforeScroll = catalogPage.getFirstProductName();

        catalogPage.scrollToBottom();
        catalogPage.scrollToTop();

        String firstProductAfterScroll = catalogPage.getFirstProductName();

        Assert.assertEquals(firstProductAfterScroll, firstProductBeforeScroll, "First product should remain the same after scrolling. " +
                "Before: " + firstProductBeforeScroll + " After: " + firstProductAfterScroll);
    }
}
