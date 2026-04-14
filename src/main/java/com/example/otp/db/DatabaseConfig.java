package com.example.otp.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {
  private static final Properties props = new Properties();

  private DatabaseConfig() {
    throw new UnsupportedOperationException("Utility class");
  }

  static {
    try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
      if (input == null) {
        throw new IllegalStateException("db.properties not found");
      }

      props.load(input);
    } catch (IOException e) {
        throw new IllegalStateException("Failed to load DB config", e);
    }
  }

  public static String get(String key) {
    return props.getProperty(key);
  }
}
