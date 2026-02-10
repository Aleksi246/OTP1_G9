// java
package com.example.otp.dao;

import com.example.otp.db.Database;
import com.example.otp.model.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserDaoTest {

    private static UserDao dao;

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
        dao = new UserDao();
    }

    @Test
    public void testCreateFindUpdateDelete() throws SQLException {
        // create
        User u = new User();
        String base = "testuser_" + System.currentTimeMillis();
        u.setUsername(base);
        u.setPasswordHash("hash");
        u.setUserType("student");
        u.setAccessToken(null);

        User created = dao.create(u);
        assertNotNull(created.getUserId(), "created user id should be set");

        // findById
        User found = dao.findById(created.getUserId());
        assertNotNull(found);
        assertEquals(base, found.getUsername());
        assertEquals("student", found.getUserType());

        // findAll contains the new user
        List<User> all = dao.findAll();
        boolean contains = all.stream().anyMatch(x -> x.getUserId().equals(created.getUserId()));
        assertTrue(contains, "findAll should contain created user");

        // update
        created.setUsername(base + "_upd");
        created.setPasswordHash("newhash");
        created.setUserType("teacher");
        created.setAccessToken("tok");
        assertTrue(dao.update(created), "update should return true");

        User updated = dao.findById(created.getUserId());
        assertNotNull(updated);
        assertEquals(base + "_upd", updated.getUsername());
        assertEquals("teacher", updated.getUserType());
        assertEquals("tok", updated.getAccessToken());

        // delete
        assertTrue(dao.delete(created.getUserId()), "delete should return true");
        assertNull(dao.findById(created.getUserId()), "deleted user should not be found");
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