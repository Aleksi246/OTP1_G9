package com.example.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginResultTest {

    @Test
    void successContainsAllFields() {
        LoginResult result = LoginResult.success("tok123", "alice", "alice@test.com", "teacher");

        assertTrue(result.isSuccess());
        assertEquals("tok123", result.getToken());
        assertEquals("alice", result.getUsername());
        assertEquals("alice@test.com", result.getEmail());
        assertEquals("teacher", result.getUserType());
        assertNull(result.getErrorKey());
        assertNull(result.getErrorArgs());
    }

    @Test
    void errorWithoutArgs() {
        LoginResult result = LoginResult.error("login.error.failed");

        assertFalse(result.isSuccess());
        assertNull(result.getToken());
        assertNull(result.getUsername());
        assertNull(result.getEmail());
        assertNull(result.getUserType());
        assertEquals("login.error.failed", result.getErrorKey());
        assertNotNull(result.getErrorArgs());
        assertEquals(0, result.getErrorArgs().length);
    }

    @Test
    void errorWithArgs() {
        LoginResult result = LoginResult.error("login.error.failed", 401);

        assertFalse(result.isSuccess());
        assertEquals("login.error.failed", result.getErrorKey());
        assertEquals(1, result.getErrorArgs().length);
        assertEquals(401, result.getErrorArgs()[0]);
    }

    @Test
    void errorMultipleArgs() {
        LoginResult result = LoginResult.error("some.error", "arg1", 42, true);

        assertEquals(3, result.getErrorArgs().length);
        assertEquals("arg1", result.getErrorArgs()[0]);
        assertEquals(42, result.getErrorArgs()[1]);
        assertEquals(true, result.getErrorArgs()[2]);
    }

    @Test
    void successWithNullUserType() {
        LoginResult result = LoginResult.success("tok", "user", "e@e.com", null);
        assertTrue(result.isSuccess());
        assertNull(result.getUserType());
    }
}
