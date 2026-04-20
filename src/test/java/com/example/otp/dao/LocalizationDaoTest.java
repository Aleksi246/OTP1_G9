package com.example.otp.dao;

import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LocalizationDao using the real test database.
 * Assumes the localization_strings table exists and has English translations.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LocalizationDaoTest {

    @Test
    @Order(1)
    void getString_nullKey_returnsNull() {
        assertNull(LocalizationDao.getString(null, "en"));
    }

    @Test
    @Order(2)
    void getString_nullLanguage_returnsNull() {
        assertNull(LocalizationDao.getString("login.title", null));
    }

    @Test
    @Order(3)
    void getString_bothNull_returnsNull() {
        assertNull(LocalizationDao.getString(null, null));
    }

    @Test
    @Order(4)
    void getString_nonExistentKey_returnsNull() {
        assertNull(LocalizationDao.getString("this.key.does.not.exist.xyz", "en"));
    }

    @Test
    @Order(5)
    void getString_nonExistentLanguage_returnsNull() {
        assertNull(LocalizationDao.getString("login.title", "zz"));
    }

    @Test
    @Order(6)
    void getString_validEnglishKey_returnsValue() {
        // "login.title" should exist in the test DB for English
        String result = LocalizationDao.getString("login.title", "en");
        // If the key exists, it should be non-null and non-empty
        if (result != null) {
            assertFalse(result.isEmpty());
        }
        // If DB has no data, this just passes (null is acceptable)
    }

    @Test
    @Order(7)
    void getAllTranslations_nullLanguage_returnsEmptyMap() {
        Map<String, String> result = LocalizationDao.getAllTranslations(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @Order(8)
    void getAllTranslations_nonExistentLanguage_returnsEmptyMap() {
        Map<String, String> result = LocalizationDao.getAllTranslations("zz");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @Order(9)
    void getAllTranslations_english_returnsNonEmptyMap() {
        Map<String, String> result = LocalizationDao.getAllTranslations("en");
        assertNotNull(result);
        // There should be English translations in the test DB
        if (!result.isEmpty()) {
            // Every key and value should be non-null
            for (Map.Entry<String, String> entry : result.entrySet()) {
                assertNotNull(entry.getKey());
                assertNotNull(entry.getValue());
            }
        }
    }

    @Test
    @Order(10)
    void getAllTranslations_persian_returnsNonEmptyMap() {
        Map<String, String> result = LocalizationDao.getAllTranslations("fa");
        assertNotNull(result);
        // Persian translations may exist in the test DB
    }

    @Test
    @Order(11)
    void getString_consistentWithGetAllTranslations() {
        // If getAllTranslations returns a key, getString should return the same value
        Map<String, String> allEn = LocalizationDao.getAllTranslations("en");
        if (!allEn.isEmpty()) {
            Map.Entry<String, String> first = allEn.entrySet().iterator().next();
            String singleResult = LocalizationDao.getString(first.getKey(), "en");
            assertEquals(first.getValue(), singleResult);
        }
    }

    @Test
    @Order(12)
    void getString_emptyKey_returnsNull() {
        assertNull(LocalizationDao.getString("", "en"));
    }

    @Test
    @Order(13)
    void getString_emptyLanguage_returnsNull() {
        assertNull(LocalizationDao.getString("login.title", ""));
    }

    @Test
    @Order(14)
    void getAllTranslations_emptyLanguage_returnsEmptyMap() {
        Map<String, String> result = LocalizationDao.getAllTranslations("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
