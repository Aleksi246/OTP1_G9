package com.example.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;

class LocaleManagerTest {

    @BeforeEach
    void resetLocale() {
        // Reset to English before each test
        LocaleManager.setLocale(Locale.ENGLISH);
    }

    // =========================================================================
    // getLocale / setLocale
    // =========================================================================

    @Test
    void getLocaleDefaultIsEnglish() {
        LocaleManager.setLocale(Locale.ENGLISH);
        assertEquals(Locale.ENGLISH, LocaleManager.getLocale());
    }

    @Test
    void setLocaleChangesCurrentLocale() {
        LocaleManager.setLocale(Locale.FRENCH);
        assertEquals(Locale.FRENCH, LocaleManager.getLocale());
    }

    @Test
    void setLocaleNullResetsToEnglish() {
        LocaleManager.setLocale(Locale.FRENCH);
        LocaleManager.setLocale(null);
        assertEquals(Locale.ENGLISH, LocaleManager.getLocale());
    }

    // =========================================================================
    // getString
    // =========================================================================

    @Test
    void getStringReturnsKeyWhenTranslationMissing() {
        String result = LocaleManager.getString("nonexistent.key.xyz");
        assertEquals("nonexistent.key.xyz", result, "Should fall back to returning the key itself");
    }

    @Test
    void getStringReturnsNullForNullKey() {
        assertNull(LocaleManager.getString((String) null));
    }

    @Test
    void getStringWithParamsFormatsWhenTranslationExists() {
        // When key is not in translations, getString returns the key itself
        // MessageFormat on a plain string with no placeholders returns it as-is
        String result = LocaleManager.getString("some.key", "param1");
        assertNotNull(result);
    }

    @Test
    void getStringWithParamsNoParamsReturnsTranslation() {
        String result = LocaleManager.getString("some.key");
        assertEquals("some.key", result);
    }

    @Test
    void getStringWithEmptyParamsReturnsTranslation() {
        String result = LocaleManager.getString("some.key", new Object[]{});
        assertEquals("some.key", result);
    }

    // =========================================================================
    // getBundle
    // =========================================================================

    @Test
    void getBundleReturnsNonNull() {
        ResourceBundle bundle = LocaleManager.getBundle();
        assertNotNull(bundle);
    }

    @Test
    void getBundleHandleGetObjectReturnsKeyForMissingTranslation() {
        ResourceBundle bundle = LocaleManager.getBundle();
        String result = bundle.getString("missing.key.abc");
        assertEquals("missing.key.abc", result);
    }

    @Test
    void getBundleContainsKeyTrueForNonNull() {
        ResourceBundle bundle = LocaleManager.getBundle();
        assertTrue(bundle.containsKey("any.key"));
    }

    @Test
    void getBundleContainsKeyFalseForNull() {
        ResourceBundle bundle = LocaleManager.getBundle();
        assertFalse(bundle.containsKey(null));
    }

    @Test
    void getBundleGetKeysReturnsEnumeration() {
        ResourceBundle bundle = LocaleManager.getBundle();
        assertNotNull(bundle.getKeys());
    }

    // =========================================================================
    // isRightToLeft
    // =========================================================================

    @Test
    void isRightToLeftFalseForEnglish() {
        LocaleManager.setLocale(Locale.ENGLISH);
        assertFalse(LocaleManager.isRightToLeft());
    }

    @Test
    void isRightToLeftTrueForPersian() {
        LocaleManager.setLocale(Locale.of("fa"));
        assertTrue(LocaleManager.isRightToLeft());
    }

    @Test
    void isRightToLeftFalseForFrench() {
        LocaleManager.setLocale(Locale.FRENCH);
        assertFalse(LocaleManager.isRightToLeft());
    }

    // =========================================================================
    // refresh
    // =========================================================================

    @Test
    void refreshClearsAndReloadsCache() {
        // Should not throw
        assertDoesNotThrow(() -> LocaleManager.refresh());
    }

    // =========================================================================
    // Locale switching reloads translations
    // =========================================================================

    @Test
    void setLocaleToPersianAndBack() {
        LocaleManager.setLocale(Locale.of("fa"));
        assertEquals("fa", LocaleManager.getLocale().getLanguage());

        LocaleManager.setLocale(Locale.ENGLISH);
        assertEquals("en", LocaleManager.getLocale().getLanguage());
    }

    @Test
    void setLocaleToRussian() {
        LocaleManager.setLocale(Locale.of("ru"));
        assertEquals("ru", LocaleManager.getLocale().getLanguage());
        assertFalse(LocaleManager.isRightToLeft());
    }
}
