package io.mobilytix.exceptions;

public class ApkNotFoundException extends RuntimeException {
    public ApkNotFoundException(String resolvedPath) {
        super("APK not found at: " + resolvedPath + "\n" + "Ensure the APK is placed in the apks/ folder and apk_path in config.yaml is correct.");
    }
}
