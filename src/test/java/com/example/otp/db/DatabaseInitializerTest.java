package com.example.otp.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseInitializerTest {

    @Test
    void initializeWithFile_validSqlFile_doesNotThrow() {
        // init.sql exists at project root and is idempotent
        assertDoesNotThrow(() -> DatabaseInitializer.initializeWithFile("init.sql"));
    }

    @Test
    void initializeWithFile_nonExistentFile_doesNotThrow() {
        // Should handle missing file gracefully (logs error, doesn't throw)
        assertDoesNotThrow(() -> DatabaseInitializer.initializeWithFile("nonexistent_file_xyz.sql"));
    }

    @Test
    void initializeWithFile_emptyFile_doesNotThrow(@TempDir Path tempDir) throws Exception {
        Path emptySQL = tempDir.resolve("empty.sql");
        Files.writeString(emptySQL, "");
        assertDoesNotThrow(() -> DatabaseInitializer.initializeWithFile(emptySQL.toString()));
    }

    @Test
    void initializeWithFile_singleStatement(@TempDir Path tempDir) throws Exception {
        Path sqlFile = tempDir.resolve("test.sql");
        Files.writeString(sqlFile, "SELECT 1;");
        assertDoesNotThrow(() -> DatabaseInitializer.initializeWithFile(sqlFile.toString()));
    }

    @Test
    void initializeWithFile_multipleStatements(@TempDir Path tempDir) throws Exception {
        Path sqlFile = tempDir.resolve("multi.sql");
        Files.writeString(sqlFile, "SELECT 1;\nSELECT 2;\nSELECT 3;");
        assertDoesNotThrow(() -> DatabaseInitializer.initializeWithFile(sqlFile.toString()));
    }

    @Test
    void initializeWithFile_invalidSql(@TempDir Path tempDir) throws Exception {
        // Invalid SQL should be handled gracefully (logged, not thrown)
        Path sqlFile = tempDir.resolve("bad.sql");
        Files.writeString(sqlFile, "THIS IS NOT VALID SQL;");
        assertDoesNotThrow(() -> DatabaseInitializer.initializeWithFile(sqlFile.toString()));
    }

    @Test
    void initializeDatabase_doesNotThrow() {
        // Uses default init.sql path
        assertDoesNotThrow(DatabaseInitializer::initializeDatabase);
    }
}
