package com.example.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    @AfterEach
    void cleanup() {
        SessionManager.clearSession();
    }

    @Test
    void setSessionStoresAllFields() {
        SessionManager.setSession("alice", "alice@test.com", "tok123", "teacher");

        assertEquals("alice", SessionManager.getUsername());
        assertEquals("alice@test.com", SessionManager.getEmail());
        assertEquals("tok123", SessionManager.getToken());
        assertEquals("teacher", SessionManager.getUserType());
    }

    @Test
    void setUserIdAndGetUserId() {
        SessionManager.setUserId(42);
        assertEquals(42, SessionManager.getUserId());
    }

    @Test
    void clearSessionRemovesAllFields() {
        SessionManager.setSession("bob", "bob@test.com", "tok456", "student");
        SessionManager.setUserId(7);
        SessionManager.clearSession();

        assertNull(SessionManager.getUsername());
        assertNull(SessionManager.getEmail());
        assertNull(SessionManager.getToken());
        assertNull(SessionManager.getUserType());
        // Note: clearSession does not clear userId per implementation
    }

    @Test
    void isLoggedInTrueWhenTokenPresent() {
        SessionManager.setSession("u", "e", "validtoken", "s");
        assertTrue(SessionManager.isLoggedIn());
    }

    @Test
    void isLoggedInFalseWhenTokenNull() {
        SessionManager.clearSession();
        assertFalse(SessionManager.isLoggedIn());
    }

    @Test
    void isLoggedInFalseWhenTokenEmpty() {
        SessionManager.setSession("u", "e", "", "s");
        assertFalse(SessionManager.isLoggedIn());
    }

    @Test
    void gettersReturnNullBeforeAnySession() {
        SessionManager.clearSession();
        assertNull(SessionManager.getUsername());
        assertNull(SessionManager.getEmail());
        assertNull(SessionManager.getToken());
        assertNull(SessionManager.getUserType());
    }

    @Test
    void setSessionOverwritesPreviousValues() {
        SessionManager.setSession("first", "first@test.com", "tok1", "student");
        SessionManager.setSession("second", "second@test.com", "tok2", "teacher");

        assertEquals("second", SessionManager.getUsername());
        assertEquals("second@test.com", SessionManager.getEmail());
        assertEquals("tok2", SessionManager.getToken());
        assertEquals("teacher", SessionManager.getUserType());
    }

    @Test
    void setUserIdOverwritesPreviousValue() {
        SessionManager.setUserId(1);
        SessionManager.setUserId(2);
        assertEquals(2, SessionManager.getUserId());
    }

    @Test
    void setUserIdAcceptsNull() {
        SessionManager.setUserId(5);
        SessionManager.setUserId(null);
        assertNull(SessionManager.getUserId());
    }
}