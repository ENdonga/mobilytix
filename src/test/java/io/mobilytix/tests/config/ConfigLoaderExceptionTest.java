package io.mobilytix.tests.config;

import io.mobilytix.config.ConfigLoader;
import io.mobilytix.exceptions.ConfigException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConfigLoaderExceptionTest {
    private final ConfigLoader config = ConfigLoader.getInstance();

    @Test(description = "Throws ConfigException when a valid top-level key is missing")
    public void testMissingTopLevelKeyThrowsConfigException() {
        ConfigException ex = Assert.expectThrows(ConfigException.class, () -> config.getNestedValue("non_existing_key"));
        Assert.assertTrue(ex.getMessage().contains("non_existing_key"), "Exception message should contain missing key name");
    }

    @Test(description = "Throws ConfigException when a nested key is missing")
    public void testMissingNestedKeyThrowsConfigException() {
        ConfigException ex = Assert.expectThrows(ConfigException.class, () -> config.getNestedValue("framework", "non_existent_nested_key"));
        Assert.assertTrue(ex.getMessage().contains("non_existent_nested_key"), "Exception message should contain the missing nested key name");
    }

    @Test(description = "Throws ConfigException when traversal hits a non-map value")
    public void testNonMapParentThrowsConfigException() {
        // "host" is a string value under framework.appium
        // Trying to traverse into it as if it were a map should throw
        ConfigException ex = Assert.expectThrows(ConfigException.class, () -> config.getNestedValue("framework", "appium", "host", "invalid_child"));
        Assert.assertTrue(ex.getMessage().contains("invalid_child"), "Exception message should contain the key that caused the traversal failure");
    }

    @Test(description = "Throws ConfigException when app key does not exist in config")
    public void testUnknownAppKeyThrowsConfigException() {
        ConfigException ex = Assert.expectThrows(ConfigException.class, () -> config.getAppConfig("non_existent_app"));
        Assert.assertTrue(ex.getMessage().contains("non_existent_app"), "Exception message should contain the unknown app key");
    }

    @Test(description = "Valid nested key path returns value without exception")
    public void testValidKeyPathReturnsValue() {
        String host = config.getNestedValue("framework", "appium", "host");
        Assert.assertNotNull(host, "Host value should not be null");
        Assert.assertFalse(host.isBlank(), "Host value should not be blank");
    }

    @Test(description = "Valid app key loads AppConfig without exception")
    public void testValidAppKeyLoadsConfig() {
        Assert.assertNotNull(config.getAppConfig("app_a"), "AppConfig should load successfully for app_a");
    }
}
