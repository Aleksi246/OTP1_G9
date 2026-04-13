package com.example.otp.db;

import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {
  private static final Properties props = new Properties();

  static {
    try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
      if (input == null) {
        throw new RuntimeException("db.properties not found");
      }

      props.load(input);
    } catch (Exception e) {
        throw new RuntimeException("Failed to load DB config", e);
    }
  }

  public static String get(String key) {
    return props.getProperty(key);
  }
}
