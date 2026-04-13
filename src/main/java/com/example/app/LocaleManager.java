package com.example.app;

import com.example.otp.dao.LocalizationDao;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class LocaleManager {
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final String DEFAULT_LANGUAGE = DEFAULT_LOCALE.getLanguage();

    // Cache translations per language to avoid per-key DB queries.
    private static final Map<String, Map<String, String>> translationsByLanguage = new HashMap<>();
    private static volatile Map<String, String> currentTranslations = Collections.emptyMap();
    private static Locale currentLocale = DEFAULT_LOCALE;

    // Single bundle instance that always resolves values from the active cache.
    private static final ResourceBundle BUNDLE = new ResourceBundle() {
        @Override
        protected Object handleGetObject(String key) {
            if (key == null) {
                return null;
            }
            String value = currentTranslations.get(key);
            // Never return null here; FXMLLoader treats missing bundle keys as fatal.
            return value != null ? value : key;
        }

        @Override
        public boolean containsKey(String key) {
            // Allow FXML %key resolution to gracefully fall back when DB rows are missing.
            return key != null;
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(currentTranslations.keySet());
        }
    };

    static {
        reloadCurrentTranslations();
    }

    public static synchronized Locale getLocale() {
        return currentLocale;
    }

    public static synchronized void setLocale(Locale locale) {
        currentLocale = (locale == null) ? DEFAULT_LOCALE : locale;
        reloadCurrentTranslations();
    }

    /**
     * Clears cached translations and reloads the active locale.
     * Useful when DB translation rows are changed at runtime.
     */
    public static synchronized void refresh() {
        translationsByLanguage.clear();
        reloadCurrentTranslations();
    }

    /**
     * Gets a translated string from the active in-memory cache.
     * Falls back to the key when no translation exists.
     */
    public static String getString(String key) {
        if (key == null) {
            return null;
        }
        String translation = currentTranslations.get(key);
        return translation != null ? translation : key;
    }

    /**
     * Gets a translated string with formatted parameters
     * @param key the translation key
     * @param params the parameters to format into the string
     * @return the formatted translated string
     */
    public static String getString(String key, Object... params) {
        String translation = getString(key);
        if (params.length > 0) {
            try {
                return MessageFormat.format(translation, params);
            } catch (Exception e) {
                // If formatting fails, return the translation as-is
                return translation;
            }
        }
        return translation;
    }

    /**
     * Returns a ResourceBundle compatible with FXMLLoader.
     * Values are served from the in-memory translation cache.
     */
    public static ResourceBundle getBundle() {
        return BUNDLE;
    }

    /**
     * Checks if the current locale is right-to-left (Persian)
     */
    public static boolean isRightToLeft() {
        return currentLocale != null && "fa".equals(currentLocale.getLanguage());
    }

    private static synchronized void reloadCurrentTranslations() {
        String languageCode = normalizeLanguage(currentLocale);
        currentTranslations = getOrLoadTranslations(languageCode);
    }

    private static synchronized Map<String, String> getOrLoadTranslations(String languageCode) {
        Map<String, String> cached = translationsByLanguage.get(languageCode);
        if (cached != null) {
            return cached;
        }

        Map<String, String> languageTranslations = LocalizationDao.getAllTranslations(languageCode);
        Map<String, String> merged = new HashMap<>();

        // Use English as fallback for missing keys in non-default locales.
        if (!DEFAULT_LANGUAGE.equals(languageCode)) {
            merged.putAll(getOrLoadTranslations(DEFAULT_LANGUAGE));
        }
        merged.putAll(languageTranslations);

        Map<String, String> immutable = Collections.unmodifiableMap(merged);
        translationsByLanguage.put(languageCode, immutable);
        return immutable;
    }

    private static String normalizeLanguage(Locale locale) {
        if (locale == null || locale.getLanguage() == null || locale.getLanguage().isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        return locale.getLanguage().toLowerCase(Locale.ROOT);
    }
}
