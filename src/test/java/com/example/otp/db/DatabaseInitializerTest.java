package com.example.otp.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseInitializerTest {

    @Test
    void initializeWithFileValidSqlFileDoesNotThrow() {
        // init.sql exists at project root and is idempotent
        assertDoesNotThrow(() -> DatabaseInitializer.initializeWithFile("init.sql"));
    }

    @Test
    void initializeWithFileNonExistentFileDoesNotThrow() {
        // Should handle missing file gracefully (logs error, doesn't throw)
        assertDoesNotThrow(() -> DatabaseInitializer.initializeWithFile("nonexistent_file_xyz.sql"));
    }

    @Test
    void initializeWithFileEmptyFileDoesNotThrow(@TempDir Path tempDir) throws Exception {
        Path emptySQL = tempDir.resolve("empty.sql");
        Files.writeString(emptySQL, "");
        assertDoesNotThrow(() -> DatabaseInitializer.initializeWithFile(emptySQL.toString()));
    }

    @Test
    void initializeWithFileSingleStatement(@TempDir Path tempDir) throws Exception {
        Path sqlFile = tempDir.resolve("test.sql");
        Files.writeString(sqlFile, "SELECT 1;");
        assertDoesNotThrow(() -> DatabaseInitializer.initializeWithFile(sqlFile.toString()));
    }

    @Test
    void initializeWithFileMultipleStatements(@TempDir Path tempDir) throws Exception {
        Path sqlFile = tempDir.resolve("multi.sql");
        Files.writeString(sqlFile, "SELECT 1;\nSELECT 2;\nSELECT 3;");
        assertDoesNotThrow(() -> DatabaseInitializer.initializeWithFile(sqlFile.toString()));
    }

    @Test
    void initializeWithFileInvalidSql(@TempDir Path tempDir) throws Exception {
        // Invalid SQL should be handled gracefully (logged, not thrown)
        Path sqlFile = tempDir.resolve("bad.sql");
        Files.writeString(sqlFile, "THIS IS NOT VALID SQL;");
        assertDoesNotThrow(() -> DatabaseInitializer.initializeWithFile(sqlFile.toString()));
    }

    @Test
    void initializeDatabaseDoesNotThrow() {
        // Uses default init.sql path
        assertDoesNotThrow(DatabaseInitializer::initializeDatabase);
    }
}
