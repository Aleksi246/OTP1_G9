package com.example.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
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
        return ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale, new UTF8Control());
    }

    public static boolean isRightToLeft() {
        return currentLocale != null && "fa".equals(currentLocale.getLanguage());
    }

    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream == null) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return new PropertyResourceBundle(reader);
            }
        }
    }
}
