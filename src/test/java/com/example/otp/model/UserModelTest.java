package com.example.otp.model;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class UserModelTest {

    @Test
    void defaultConstructor() {
        User u = new User();
        assertNull(u.getUserId());
        assertNull(u.getUsername());
        assertNull(u.getPasswordHash());
        assertNull(u.getEmail());
        assertNull(u.getCreatedAt());
    }

    @Test
    void parameterizedConstructor() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        User u = new User(1, "john", "hash123", "john@test.com", ts);
        assertEquals(1, u.getUserId());
        assertEquals("john", u.getUsername());
        assertEquals("hash123", u.getPasswordHash());
        assertEquals("john@test.com", u.getEmail());
        assertEquals(ts, u.getCreatedAt());
    }

    @Test
    void setUserId() {
        User u = new User();
        u.setUserId(42);
        assertEquals(42, u.getUserId());
    }

    @Test
    void setUsername() {
        User u = new User();
        u.setUsername("alice");
        assertEquals("alice", u.getUsername());
    }

    @Test
    void setPasswordHash() {
        User u = new User();
        u.setPasswordHash("$2a$10$abc");
        assertEquals("$2a$10$abc", u.getPasswordHash());
    }

    @Test
    void setEmail() {
        User u = new User();
        u.setEmail("alice@example.com");
        assertEquals("alice@example.com", u.getEmail());
    }

    @Test
    void setCreatedAt() {
        User u = new User();
        Timestamp ts = new Timestamp(12345L);
        u.setCreatedAt(ts);
        assertEquals(ts, u.getCreatedAt());
    }

    @Test
    void setNullValues() {
        User u = new User(1, "x", "y", "z", new Timestamp(0));
        u.setUserId(null);
        u.setUsername(null);
        u.setPasswordHash(null);
        u.setEmail(null);
        u.setCreatedAt(null);
        assertNull(u.getUserId());
        assertNull(u.getUsername());
        assertNull(u.getPasswordHash());
        assertNull(u.getEmail());
        assertNull(u.getCreatedAt());
    }
}
