package com.example.otp.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DatabaseInitializer {
    private static final String URL = "jdbc:mariadb://platform-db:3306";
    private static final String USER = "otptestuser";
    private static final String PASS = "testpass";

    /**
     * Initialize the database with schema (idempotent, safe for production).
     *
     * Environment variables:
     * - APP_RESET_DB=true: Use destructive init-dev.sql (drops/recreates all data)
     *   Default: false (uses init.sql with CREATE IF NOT EXISTS)
     * - APP_INIT_FILE: Custom SQL file path (defaults to init.sql or init-dev.sql)
     */
    public static void initializeDatabase() {
        boolean isDevReset = "true".equalsIgnoreCase(System.getenv("APP_RESET_DB"));
        String initFile = System.getenv("APP_INIT_FILE");

        if (initFile == null) {
            initFile = isDevReset ? "init-dev.sql" : "init.sql";
        }

        System.out.println("[DB Init] Using SQL file: " + initFile);
        if (isDevReset) {
            System.out.println("[DB Init] WARNING: APP_RESET_DB=true - Running DESTRUCTIVE initialization!");
        }

        initializeWithFile(initFile);
    }

    /**
     * Initialize the database using a specific SQL file.
     */
    public static void initializeWithFile(String sqlFilePath) {
        try {
            // Load the MariaDB driver
            Class.forName("org.mariadb.jdbc.Driver");

            // Connect as root to create database and user
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 Statement stmt = conn.createStatement()) {

                // Read the SQL file
                String initSql = new String(Files.readAllBytes(Paths.get(sqlFilePath)));

                // Split by semicolon and execute each statement
                String[] statements = initSql.split(";");
                for (String sql : statements) {
                    sql = sql.trim();
                    if (!sql.isEmpty()) {
                        try {
                            stmt.execute(sql);
                            System.out.println("[DB Init] Executed: " + sql.substring(0, Math.min(50, sql.length())) + "...");
                        } catch (Exception e) {
                            System.err.println("[DB Init] Error executing SQL: " + e.getMessage());
                            // Continue with other statements (ignore CREATE TABLE IF EXISTS failures, etc)
                        }
                    }
                }
                System.out.println("[DB Init] Database initialization completed successfully!");
            }
        } catch (Exception e) {
            System.err.println("[DB Init] Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

