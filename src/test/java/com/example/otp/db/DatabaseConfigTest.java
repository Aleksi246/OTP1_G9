package com.example.otp.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @Test
    void get_dbUser_returnsNonNull() {
        String user = DatabaseConfig.get("db.user");
        assertNotNull(user, "db.user should be configured in db.properties");
    }

    @Test
    void get_dbPass_returnsNonNull() {
        String pass = DatabaseConfig.get("db.pass");
        assertNotNull(pass, "db.pass should be configured in db.properties");
    }

    @Test
    void get_nonExistentKey_returnsNull() {
        assertNull(DatabaseConfig.get("nonexistent.key.xyz"));
    }

    @Test
    void get_nullKey_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DatabaseConfig.get(null));
    }

    @Test
    void constructor_throwsUnsupportedOperationException() {
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
