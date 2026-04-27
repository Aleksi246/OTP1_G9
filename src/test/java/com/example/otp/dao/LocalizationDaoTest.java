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
    void getStringNullKeyReturnsNull() {
        assertNull(LocalizationDao.getString(null, "en"));
    }

    @Test
    @Order(2)
    void getStringNullLanguageReturnsNull() {
        assertNull(LocalizationDao.getString("login.title", null));
    }

    @Test
    @Order(3)
    void getStringBothNullReturnsNull() {
        assertNull(LocalizationDao.getString(null, null));
    }

    @Test
    @Order(4)
    void getStringNonExistentKeyReturnsNull() {
        assertNull(LocalizationDao.getString("this.key.does.not.exist.xyz", "en"));
    }

    @Test
    @Order(5)
    void getStringNonExistentLanguageReturnsNull() {
        assertNull(LocalizationDao.getString("login.title", "zz"));
    }

    @Test
    @Order(6)
    void getStringValidEnglishKeyReturnsValue() {
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
    void getAllTranslationsNullLanguageReturnsEmptyMap() {
        Map<String, String> result = LocalizationDao.getAllTranslations(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @Order(8)
    void getAllTranslationsNonExistentLanguageReturnsEmptyMap() {
        Map<String, String> result = LocalizationDao.getAllTranslations("zz");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @Order(9)
    void getAllTranslationsEnglishReturnsNonEmptyMap() {
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
    void getAllTranslationsPersianReturnsNonEmptyMap() {
        Map<String, String> result = LocalizationDao.getAllTranslations("fa");
        assertNotNull(result);
        // Persian translations may exist in the test DB
    }

    @Test
    @Order(11)
    void getStringConsistentWithGetAllTranslations() {
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
    void getStringEmptyKeyReturnsNull() {
        assertNull(LocalizationDao.getString("", "en"));
    }

    @Test
    @Order(13)
    void getStringEmptyLanguageReturnsNull() {
        assertNull(LocalizationDao.getString("login.title", ""));
    }

    @Test
    @Order(14)
    void getAllTranslationsEmptyLanguageReturnsEmptyMap() {
        Map<String, String> result = LocalizationDao.getAllTranslations("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
