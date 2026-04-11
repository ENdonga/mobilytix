package io.mobilytix.exceptions;

public class DeviceNotReadyException extends RuntimeException {
    public DeviceNotReadyException(String udid) {
        super("Device '" + udid + "' is not connected or not ready.\nFix options:\n" +
                "  1. Start your emulator in Android Studio → Device Manager\n" +
                "  2. Connect your physical device via USB\n" +
                "  3. Run `adb devices` to see connected devices\n" +
                "  4. Update udid in config.yaml if using a different device");
    }
}
