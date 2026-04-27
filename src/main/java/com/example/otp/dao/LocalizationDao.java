package com.example.otp.dao;

import com.example.otp.db.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

public class LocalizationDao {
    private static final Logger logger = Logger.getLogger(LocalizationDao.class.getName());

    /**
     * Fetches a single translation string for a given key and language
     * @param key the translation key (e.g., "login.title")
     * @param language the language code (e.g., "en", "fa", "fr", "ru")
     * @return the translated string, or null if not found
     */
    public static String getString(String key, String language) {
        if (key == null || language == null) {
            return null;
        }
        
        String query = "SELECT value FROM localization_strings WHERE translation_key = ? AND language = ?";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, key);
            stmt.setString(2, language);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, e, () ->
                        "Error fetching translation for key: " + key + ", language: " + language
                );
            }
        }
        
        // Return null if translation not found (allows FXML fallback)
        return null;
    }
    
    /**
     * Fetches all translations for a given language
     * @param language the language code (e.g., "en", "fa", "fr", "ru")
     * @return a map of translation keys to values
     */
    public static Map<String, String> getAllTranslations(String language) {
        Map<String, String> translations = new HashMap<>();
        
        if (language == null) {
            return translations;
        }
        
        String query = "SELECT translation_key, value FROM localization_strings WHERE language = ?";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, language);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    translations.put(rs.getString("translation_key"), rs.getString("value"));
                }
            }
        } catch (SQLException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, e, () -> "Error fetching all translations for language: " + language);
            }
        }
        
        return translations;
    }
}

