package com.example.otp.model;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class CourseModelTest {

    @Test
    void defaultConstructor() {
        Course course = new Course();
        assertNull(course.getClassId());
        assertNull(course.getClassName());
        assertNull(course.getCreatorId());
        assertNull(course.getTopic());
        assertNull(course.getCreatedAt());
    }

    @Test
    void parameterizedConstructor() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Course course = new Course(1, "Math 101", 10, "Calculus", ts);
        assertEquals(1, course.getClassId());
        assertEquals("Math 101", course.getClassName());
        assertEquals(10, course.getCreatorId());
        assertEquals("Calculus", course.getTopic());
        assertEquals(ts, course.getCreatedAt());
    }

    @Test
    void setClassId() {
        Course course = new Course();
        course.setClassId(5);
        assertEquals(5, course.getClassId());
    }

    @Test
    void setClassName() {
        Course course = new Course();
        course.setClassName("Physics");
        assertEquals("Physics", course.getClassName());
    }

    @Test
    void setCreatorId() {
        Course course = new Course();
        course.setCreatorId(99);
        assertEquals(99, course.getCreatorId());
    }

    @Test
    void setTopic() {
        Course course = new Course();
        course.setTopic("Quantum");
        assertEquals("Quantum", course.getTopic());
    }

    @Test
    void setCreatedAt() {
        Course course = new Course();
        Timestamp ts = new Timestamp(1000L);
        course.setCreatedAt(ts);
        assertEquals(ts, course.getCreatedAt());
    }

    @Test
    void setNullValues() {
        Course course = new Course(1, "X", 2, "Y", new Timestamp(0));
        course.setClassId(null);
        course.setClassName(null);
        course.setCreatorId(null);
        course.setTopic(null);
        course.setCreatedAt(null);
        assertNull(course.getClassId());
        assertNull(course.getClassName());
        assertNull(course.getCreatorId());
        assertNull(course.getTopic());
        assertNull(course.getCreatedAt());
    }
}
