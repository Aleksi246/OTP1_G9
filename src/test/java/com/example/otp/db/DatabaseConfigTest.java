package com.example.otp.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @Test
    void getDbUserReturnsNonNull() {
        String user = DatabaseConfig.get("db.user");
        assertNotNull(user, "db.user should be configured in db.properties");
    }

    @Test
    void getDbPassReturnsNonNull() {
        String pass = DatabaseConfig.get("db.pass");
        assertNotNull(pass, "db.pass should be configured in db.properties");
    }

    @Test
    void getNonExistentKeyReturnsNull() {
        assertNull(DatabaseConfig.get("nonexistent.key.xyz"));
    }

    @Test
    void getNullKeyThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DatabaseConfig.get(null));
    }

    @Test
    void constructorThrowsUnsupportedOperationException() {
        var ex = assertThrows(Exception.class, () -> {
            var constructor = DatabaseConfig.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
        // The constructor wraps UnsupportedOperationException in InvocationTargetException
        assertTrue(ex.getCause() instanceof UnsupportedOperationException
                || ex instanceof UnsupportedOperationException);
    }
}
