package io.mobilytix.annotation;

import java.lang.annotation.*;

/**
 * Marks a test class with the app key to load from config.yaml.
 * <p>
 * Usage:
 *
 * @AppUnderTest("app_a") public class LoginTest extends BaseTest {
 * ...
 * }
 * <p>
 * The value must match a key under the `apps:` section in config.yaml.
 * If the key is not found, ConfigLoader will throw a clear error at runtime.
 * <p>
 * To add a new app:
 * 1. Add the app block to config.yaml under apps:
 * 2. Use the new key as the value here
 * No framework code changes are required.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface AppUnderTest {
    /**
     * The app key as defined in config.yaml under apps:
     * <p>
     * Example: "app_a", "app_b"
     * Must be an exact match — case sensitive, no spaces.
     */
    String value() default "";
}
