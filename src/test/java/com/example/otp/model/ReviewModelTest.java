package com.example.otp.model;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class ReviewModelTest {

    @Test
    void defaultConstructor() {
        Review r = new Review();
        assertNull(r.getReviewId());
        assertNull(r.getReview());
        assertNull(r.getRating());
        assertNull(r.getFileId());
        assertNull(r.getUserId());
        assertNull(r.getCreatedAt());
    }

    @Test
    void parameterizedConstructor() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Review r = new Review(1, "Great!", 5, 10, 20, ts);
        assertEquals(1, r.getReviewId());
        assertEquals("Great!", r.getReview());
        assertEquals(5, r.getRating());
        assertEquals(10, r.getFileId());
        assertEquals(20, r.getUserId());
        assertEquals(ts, r.getCreatedAt());
    }

    @Test
    void setReviewId() {
        Review r = new Review();
        r.setReviewId(99);
        assertEquals(99, r.getReviewId());
    }

    @Test
    void setReview() {
        Review r = new Review();
        r.setReview("Excellent work");
        assertEquals("Excellent work", r.getReview());
    }

    @Test
    void setRating() {
        Review r = new Review();
        r.setRating(3);
        assertEquals(3, r.getRating());
    }

    @Test
    void setFileId() {
        Review r = new Review();
        r.setFileId(55);
        assertEquals(55, r.getFileId());
    }

    @Test
    void setUserId() {
        Review r = new Review();
        r.setUserId(7);
        assertEquals(7, r.getUserId());
    }

    @Test
    void setCreatedAt() {
        Review r = new Review();
        Timestamp ts = new Timestamp(9999L);
        r.setCreatedAt(ts);
        assertEquals(ts, r.getCreatedAt());
    }

    @Test
    void setNullValues() {
        Review r = new Review(1, "x", 2, 3, 4, new Timestamp(0));
        r.setReviewId(null);
        r.setReview(null);
        r.setRating(null);
        r.setFileId(null);
        r.setUserId(null);
        r.setCreatedAt(null);
        assertNull(r.getReviewId());
        assertNull(r.getReview());
        assertNull(r.getRating());
        assertNull(r.getFileId());
        assertNull(r.getUserId());
        assertNull(r.getCreatedAt());
    }
}
