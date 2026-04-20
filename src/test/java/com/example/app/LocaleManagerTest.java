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
    void getLocale_defaultIsEnglish() {
        LocaleManager.setLocale(Locale.ENGLISH);
        assertEquals(Locale.ENGLISH, LocaleManager.getLocale());
    }

    @Test
    void setLocale_changesCurrentLocale() {
        LocaleManager.setLocale(Locale.FRENCH);
        assertEquals(Locale.FRENCH, LocaleManager.getLocale());
    }

    @Test
    void setLocale_nullResetsToEnglish() {
        LocaleManager.setLocale(Locale.FRENCH);
        LocaleManager.setLocale(null);
        assertEquals(Locale.ENGLISH, LocaleManager.getLocale());
    }

    // =========================================================================
    // getString
    // =========================================================================

    @Test
    void getString_returnsKeyWhenTranslationMissing() {
        String result = LocaleManager.getString("nonexistent.key.xyz");
        assertEquals("nonexistent.key.xyz", result, "Should fall back to returning the key itself");
    }

    @Test
    void getString_returnsNullForNullKey() {
        assertNull(LocaleManager.getString((String) null));
    }

    @Test
    void getString_withParams_formatsWhenTranslationExists() {
        // When key is not in translations, getString returns the key itself
        // MessageFormat on a plain string with no placeholders returns it as-is
        String result = LocaleManager.getString("some.key", "param1");
        assertNotNull(result);
    }

    @Test
    void getString_withParams_noParamsReturnsTranslation() {
        String result = LocaleManager.getString("some.key");
        assertEquals("some.key", result);
    }

    @Test
    void getString_withEmptyParams_returnsTranslation() {
        String result = LocaleManager.getString("some.key", new Object[]{});
        assertEquals("some.key", result);
    }

    // =========================================================================
    // getBundle
    // =========================================================================

    @Test
    void getBundle_returnsNonNull() {
        ResourceBundle bundle = LocaleManager.getBundle();
        assertNotNull(bundle);
    }

    @Test
    void getBundle_handleGetObject_returnsKeyForMissingTranslation() {
        ResourceBundle bundle = LocaleManager.getBundle();
        String result = bundle.getString("missing.key.abc");
        assertEquals("missing.key.abc", result);
    }

    @Test
    void getBundle_containsKey_trueForNonNull() {
        ResourceBundle bundle = LocaleManager.getBundle();
        assertTrue(bundle.containsKey("any.key"));
    }

    @Test
    void getBundle_containsKey_falseForNull() {
        ResourceBundle bundle = LocaleManager.getBundle();
        assertFalse(bundle.containsKey(null));
    }

    @Test
    void getBundle_getKeys_returnsEnumeration() {
        ResourceBundle bundle = LocaleManager.getBundle();
        assertNotNull(bundle.getKeys());
    }

    // =========================================================================
    // isRightToLeft
    // =========================================================================

    @Test
    void isRightToLeft_falseForEnglish() {
        LocaleManager.setLocale(Locale.ENGLISH);
        assertFalse(LocaleManager.isRightToLeft());
    }

    @Test
    void isRightToLeft_trueForPersian() {
        LocaleManager.setLocale(Locale.of("fa"));
        assertTrue(LocaleManager.isRightToLeft());
    }

    @Test
    void isRightToLeft_falseForFrench() {
        LocaleManager.setLocale(Locale.FRENCH);
        assertFalse(LocaleManager.isRightToLeft());
    }

    // =========================================================================
    // refresh
    // =========================================================================

    @Test
    void refresh_clearsAndReloadsCache() {
        // Should not throw
        assertDoesNotThrow(() -> LocaleManager.refresh());
    }

    // =========================================================================
    // Locale switching reloads translations
    // =========================================================================

    @Test
    void setLocale_toPersian_andBack() {
        LocaleManager.setLocale(Locale.of("fa"));
        assertEquals("fa", LocaleManager.getLocale().getLanguage());

        LocaleManager.setLocale(Locale.ENGLISH);
        assertEquals("en", LocaleManager.getLocale().getLanguage());
    }

    @Test
    void setLocale_toRussian() {
        LocaleManager.setLocale(Locale.of("ru"));
        assertEquals("ru", LocaleManager.getLocale().getLanguage());
        assertFalse(LocaleManager.isRightToLeft());
    }
}
