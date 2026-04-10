package io.mobilytix.tests;

import io.mobilytix.adb.AdbCommands;
import io.mobilytix.adb.ApkManager;
import io.mobilytix.adb.ScreenRecorder;

import java.util.List;

public class AdbSmokeTest {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Pre-flight Check ===");
        List<String> devices = AdbCommands.listConnectedDevices();
        System.out.println("Connected devices: " + devices);

//        if (devices.isEmpty()) {
//            System.err.println("No devices connected. Start your emulator first.");
//            System.err.println("Run: emulator -avd Pixel_7");
//            return;  // exit cleanly instead of crashing
//        }

        AdbCommands adb = AdbCommands.getInstance();
        ApkManager apkManager = ApkManager.getInstance();
        ScreenRecorder recorder = ScreenRecorder.getInstance();

        // -----------------------------------------------------------------
        // AdbCommands checks
        // -----------------------------------------------------------------
        System.out.println("=== Connected Devices ===");
        System.out.println("Devices found: " + devices);

        System.out.println("\n=== Device Info ===");
        System.out.println("Model  : " + adb.getDeviceModel());
        System.out.println("Android: " + adb.getAndroidVersion());

        System.out.println("\n=== Logcat (last 5 lines) ===");
        System.out.println(adb.getLogcat(5));

        System.out.println("\n=== Key Event (pressing HOME) ===");
        adb.pressHome();
        System.out.println("HOME key sent");

        // -----------------------------------------------------------------
        // ApkManager checks
        // -----------------------------------------------------------------
        System.out.println("\n=== APK Manager ===");
        boolean installed = apkManager.isInstalled("app_a");
        System.out.println("app_a installed: " + installed);

        // -----------------------------------------------------------------
        // ScreenRecorder checks
        // -----------------------------------------------------------------
        System.out.println("\n=== Screen Recorder ===");
        recorder.startRecording("AdbSmokeTest");
        System.out.println("Recording started — waiting 5 seconds...");
        Thread.sleep(5000);
        recorder.stopRecording();
        System.out.println("Recording stopped");
        System.out.println("Saved to: " + recorder.getLastRecordingFilePath());

        System.out.println("\nADB smoke test complete.");
    }
}
