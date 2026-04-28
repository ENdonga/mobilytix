package io.mobilytix.core;

/**
 * Holds suite-level state shared between BaseTest and MobilytixListener.
 * Avoids circular dependency — neither BaseTest nor MobilytixListener
 * need to import each other.
 */
public class SuiteContext {
    private static volatile boolean suiteAborted = false;
    private static volatile String suiteAbortReason = null;

    private SuiteContext() {
    }

    public static void abort(String reason) {
        suiteAborted = true;
        suiteAbortReason = reason;
    }

    public static boolean isAborted() {
        return suiteAborted;
    }

    public static String getAbortReason() {
        return suiteAbortReason;
    }

    public static void reset() {
        suiteAborted = false;
        suiteAbortReason = null;
    }
}
