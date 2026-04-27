package com.example.otp.db;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    @Test
    void getConnectionReturnsValidConnection() throws Exception {
        try (Connection conn = Database.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        }
    }

    @Test
    void getConnectionMultipleCallsReturnDifferentConnections() throws Exception {
        try (Connection conn1 = Database.getConnection();
             Connection conn2 = Database.getConnection()) {
            assertNotNull(conn1);
            assertNotNull(conn2);
            assertNotSame(conn1, conn2);
        }
    }

    @Test
    void getConnectionCanExecuteQuery() throws Exception {
        try (Connection conn = Database.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT 1")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }
}
