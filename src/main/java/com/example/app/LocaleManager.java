package com.example.app;

import java.util.Locale;
import java.util.ResourceBundle;

public class LocaleManager {
    private static Locale currentLocale = Locale.ENGLISH;
    private static final String BUNDLE_BASE_NAME = "com.example.app.messages";

    public static Locale getLocale() {
        return currentLocale;
    }

    public static void setLocale(Locale locale) {
        currentLocale = locale == null ? Locale.ENGLISH : locale;
    }

    public static ResourceBundle getBundle() {
        return ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);
    }

    public static boolean isRightToLeft() {
        return currentLocale != null && "fa".equals(currentLocale.getLanguage());
    }
}
