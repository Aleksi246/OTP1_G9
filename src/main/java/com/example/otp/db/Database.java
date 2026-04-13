package com.example.otp.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final String URL = System.getenv().getOrDefault(
    "DB_URL",
    "jdbc:mariadb://localhost:3306/otptestdb"
    );
    private static final String USER = System.getenv("DB_USER");
    private static final String PASS = System.getenv("DB_PASS");

    // Call this to obtain a new connection. Forces driver load to avoid "Driver class not found" at runtime.
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // If driver truly isn't on classpath, the stacktrace will help debugging.
            throw new SQLException("MariaDB JDBC Driver not found. Ensure dependency is on the classpath.", e);
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}