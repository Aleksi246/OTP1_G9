// java
package com.example.otp.dao;

import com.example.otp.db.Database;
import com.example.otp.model.Course;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CourseDaoTest {
    private static CourseDao dao;

    @BeforeAll
    public static void beforeClass() throws Exception {
        // Ensure a clean DB state for tests. Order matters because of FK constraints.
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM reviews");
            s.executeUpdate("DELETE FROM materials");
            s.executeUpdate("DELETE FROM participants");
            s.executeUpdate("DELETE FROM classes");
            s.executeUpdate("DELETE FROM users");
        }
        dao = new CourseDao();
    }

    @Test
    public void testCreateFindUpdateDelete() throws SQLException {
        // create
        Course c = new Course();
        String base = "testclass_" + System.currentTimeMillis();
        c.setClassName(base);
        c.setTopic("Testing");

        Course created = dao.create(c);
        assertNotNull(created.getClassId(), "created class id should be set");

        // findById
        Course found = dao.findById(created.getClassId());
        assertNotNull(found);
        assertEquals(base, found.getClassName());
        assertEquals("Testing", found.getTopic());

        // findAll contains the new class
        List<Course> all = dao.findAll();
        boolean contains = all.stream().anyMatch(x -> x.getClassId().equals(created.getClassId()));
        assertTrue(contains, "findAll should contain created class");

        // update
        created.setClassName(base + "_upd");
        created.setTopic("UpdatedTopic");
        assertTrue(dao.update(created), "update should return true");

        Course updated = dao.findById(created.getClassId());
        assertNotNull(updated);
        assertEquals(base + "_upd", updated.getClassName());
        assertEquals("UpdatedTopic", updated.getTopic());

        // delete
        assertTrue(dao.delete(created.getClassId()), "delete should return true");
        assertNull(dao.findById(created.getClassId()), "deleted class should not be found");
    }

    @AfterAll
    public static void afterClass() throws SQLException {
        // clean up created rows (defensive)
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM reviews");
            s.executeUpdate("DELETE FROM materials");
            s.executeUpdate("DELETE FROM participants");
            s.executeUpdate("DELETE FROM classes");
            s.executeUpdate("DELETE FROM users");
        }
    }
}