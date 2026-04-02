package io.mobilytix.tests;

import io.mobilytix.config.AppConfig;
import io.mobilytix.config.ConfigLoader;
import io.mobilytix.config.DeviceConfig;

public class ConfigSmokeTest {
    public static void main(String[] args) {
        ConfigLoader config = ConfigLoader.getInstance();

        System.out.println("=== Framework Config ===");
        System.out.println("Appium host: " + config.getAppiumHost());
        System.out.println("Appium port: " + config.getAppiumPort());
        System.out.println("Auto start: " + config.isAppiumAutoStart());
        System.out.println("APK base path: " + config.getApkBasePath());
        System.out.println("Explicit timeout: " + config.getExplicitTimeout() + "s");

        System.out.println("\n=== Device Config ===");
        DeviceConfig device = config.getDeviceConfig();
        System.out.println("UDID: " + device.getUdid());
        System.out.println("Platform: " + device.getPlatformName());
        System.out.println("Version: " + device.getPlatformVersion());

        System.out.println("\n=== App A Config ===");
        AppConfig appA = config.getAppConfig("app_a");
        System.out.println("App name: " + appA.getAppName());
        System.out.println("Package: " + appA.getPackageName());
        System.out.println("Requires auth: " + appA.isRequiresAuth());
        System.out.println("Auth type: " + appA.getAuthType());

        System.out.println("\n=== App B Config ===");
        AppConfig appB = config.getAppConfig("app_b");
        System.out.println("App name: " + appB.getAppName());
        System.out.println("Requires auth: " + appB.isRequiresAuth());

        System.out.println("\nConfig loaded successfully.");
    }
}
