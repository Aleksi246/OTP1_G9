package com.example.otp.model;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class MaterialModelTest {

    @Test
    void defaultConstructor() {
        Material m = new Material();
        assertNull(m.getFileId());
        assertNull(m.getOriginalFilename());
        assertNull(m.getStoredFilename());
        assertNull(m.getFilepath());
        assertNull(m.getMaterialType());
        assertNull(m.getUploadedAt());
        assertNull(m.getClassId());
        assertNull(m.getUserId());
    }

    @Test
    void parameterizedConstructor() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Material m = new Material(1, "orig.txt", "stored.txt", "/path", "Slides", ts, 10, 20);
        assertEquals(1, m.getFileId());
        assertEquals("orig.txt", m.getOriginalFilename());
        assertEquals("stored.txt", m.getStoredFilename());
        assertEquals("/path", m.getFilepath());
        assertEquals("Slides", m.getMaterialType());
        assertEquals(ts, m.getUploadedAt());
        assertEquals(10, m.getClassId());
        assertEquals(20, m.getUserId());
    }

    @Test
    void setFileId() {
        Material m = new Material();
        m.setFileId(42);
        assertEquals(42, m.getFileId());
    }

    @Test
    void setOriginalFilename() {
        Material m = new Material();
        m.setOriginalFilename("test.pdf");
        assertEquals("test.pdf", m.getOriginalFilename());
    }

    @Test
    void setStoredFilename() {
        Material m = new Material();
        m.setStoredFilename("uuid.pdf");
        assertEquals("uuid.pdf", m.getStoredFilename());
    }

    @Test
    void setFilepath() {
        Material m = new Material();
        m.setFilepath("/uploads/file.pdf");
        assertEquals("/uploads/file.pdf", m.getFilepath());
    }

    @Test
    void setMaterialType() {
        Material m = new Material();
        m.setMaterialType("Assignment");
        assertEquals("Assignment", m.getMaterialType());
    }

    @Test
    void setUploadedAt() {
        Material m = new Material();
        Timestamp ts = new Timestamp(5000L);
        m.setUploadedAt(ts);
        assertEquals(ts, m.getUploadedAt());
    }

    @Test
    void setClassId() {
        Material m = new Material();
        m.setClassId(7);
        assertEquals(7, m.getClassId());
    }

    @Test
    void setUserId() {
        Material m = new Material();
        m.setUserId(3);
        assertEquals(3, m.getUserId());
    }

    @Test
    void setNullValues() {
        Material m = new Material(1, "a", "b", "c", "d", new Timestamp(0), 1, 1);
        m.setFileId(null);
        m.setOriginalFilename(null);
        m.setStoredFilename(null);
        m.setFilepath(null);
        m.setMaterialType(null);
        m.setUploadedAt(null);
        m.setClassId(null);
        m.setUserId(null);
        assertNull(m.getFileId());
        assertNull(m.getOriginalFilename());
        assertNull(m.getStoredFilename());
        assertNull(m.getFilepath());
        assertNull(m.getMaterialType());
        assertNull(m.getUploadedAt());
        assertNull(m.getClassId());
        assertNull(m.getUserId());
    }
}
