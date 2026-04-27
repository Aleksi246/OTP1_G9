package com.example.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClassContextHolder static state management.
 */
class ClassContextHolderTest {

    @AfterEach
    void cleanUp() {
        ClassContextHolder.setClassId(null);
    }

    @Test
    void setAndGetClassId() {
        ClassContextHolder.setClassId(42);
        assertEquals(42, ClassContextHolder.getClassId());
    }

    @Test
    void getClassIdDefaultIsNull() {
        ClassContextHolder.setClassId(null);
        assertNull(ClassContextHolder.getClassId());
    }

    @Test
    void setClassIdOverwrite() {
        ClassContextHolder.setClassId(1);
        ClassContextHolder.setClassId(2);
        assertEquals(2, ClassContextHolder.getClassId());
    }

    @Test
    void setClassIdNullAfterSet() {
        ClassContextHolder.setClassId(10);
        ClassContextHolder.setClassId(null);
        assertNull(ClassContextHolder.getClassId());
    }

    @Test
    void setClassIdLargeValue() {
        ClassContextHolder.setClassId(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, ClassContextHolder.getClassId());
    }

    @Test
    void setClassIdNegativeValue() {
        ClassContextHolder.setClassId(-1);
        assertEquals(-1, ClassContextHolder.getClassId());
    }
}
