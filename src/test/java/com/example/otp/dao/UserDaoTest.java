// java
package com.example.otp.dao;

import com.example.otp.db.Database;
import com.example.otp.model.User;
import org.junit.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.*;

public class UserDaoTest {

    private static UserDao dao;

    @BeforeClass
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
        assertNotNull("created user id should be set", created.getUserId());

        // findById
        User found = dao.findById(created.getUserId());
        assertNotNull(found);
        assertEquals(base, found.getUsername());
        assertEquals("student", found.getUserType());

        // findAll contains the new user
        List<User> all = dao.findAll();
        boolean contains = all.stream().anyMatch(x -> x.getUserId().equals(created.getUserId()));
        assertTrue("findAll should contain created user", contains);

        // update
        created.setUsername(base + "_upd");
        created.setPasswordHash("newhash");
        created.setUserType("teacher");
        created.setAccessToken("tok");
        assertTrue("update should return true", dao.update(created));

        User updated = dao.findById(created.getUserId());
        assertNotNull(updated);
        assertEquals(base + "_upd", updated.getUsername());
        assertEquals("teacher", updated.getUserType());
        assertEquals("tok", updated.getAccessToken());

        // delete
        assertTrue("delete should return true", dao.delete(created.getUserId()));
        assertNull("deleted user should not be found", dao.findById(created.getUserId()));
    }

    @AfterClass
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