package io.mobilytix.exceptions;

public class PageNotLoadedException extends RuntimeException {
    private static final String MESSAGE_TEMPLATE = "Page '%s' did not load within the expected timeout. Check locators and network conditions.";

    public PageNotLoadedException(String message) {
        super(message);
    }

    public PageNotLoadedException(String pageName, Throwable cause) {
        super(String.format(MESSAGE_TEMPLATE, pageName), cause);
    }

    public static PageNotLoadedException forPage(String pageName) {
        return new PageNotLoadedException(String.format(MESSAGE_TEMPLATE, pageName));
    }
}
