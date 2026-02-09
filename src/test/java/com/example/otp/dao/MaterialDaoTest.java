// java
package com.example.otp.dao;

import com.example.otp.db.Database;
import com.example.otp.model.User;
import com.example.otp.model.Course;
import com.example.otp.model.Material;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MaterialDaoTest {
    private static MaterialDao dao;
    private static UserDao userDao;
    private static CourseDao courseDao;

    @BeforeAll
    public static void beforeClass() throws Exception {
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM reviews");
            s.executeUpdate("DELETE FROM materials");
            s.executeUpdate("DELETE FROM participants");
            s.executeUpdate("DELETE FROM classes");
            s.executeUpdate("DELETE FROM users");
        }
        dao = new MaterialDao();
        userDao = new UserDao();
        courseDao = new CourseDao();
    }

    @Test
    public void testCreateFindUpdateDelete() throws SQLException {
        // prepare user and class
        User u = new User();
        String uname = "material_user_" + System.currentTimeMillis();
        u.setUsername(uname);
        u.setPasswordHash("hash");
        u.setUserType("teacher");
        User createdUser = userDao.create(u);
        assertNotNull(createdUser.getUserId());

        Course c = new Course();
        String cname = "material_class_" + System.currentTimeMillis();
        c.setClassName(cname);
        c.setTopic("Topic");
        Course createdClass = courseDao.create(c);
        assertNotNull(createdClass.getClassId());

        // create material
        Material m = new Material();
        m.setOriginalFilename("file.txt");
        m.setStoredFilename("stored-" + System.currentTimeMillis());
        m.setFilepath("/tmp/" + System.currentTimeMillis() + ".txt");
        m.setMaterialType("TXT");
        m.setClassId(createdClass.getClassId());
        m.setUserId(createdUser.getUserId());

        Material created = dao.create(m);
        assertNotNull(created.getFileId());

        // findById
        Material found = dao.findById(created.getFileId());
        assertNotNull(found);
        assertEquals("file.txt", found.getOriginalFilename());
        assertEquals(created.getClassId(), found.getClassId());

        // findByClassId
        List<Material> byClass = dao.findByClassId(createdClass.getClassId());
        boolean contains = byClass.stream().anyMatch(x -> x.getFileId().equals(created.getFileId()));
        assertTrue(contains, "findByClassId should contain created material");

        // update
        created.setOriginalFilename("file_updated.txt");
        created.setMaterialType("TEXT");
        assertTrue(dao.update(created), "update should return true");

        Material updated = dao.findById(created.getFileId());
        assertNotNull(updated);
        assertEquals("file_updated.txt", updated.getOriginalFilename());
        assertEquals("TEXT", updated.getMaterialType());

        // delete
        assertTrue(dao.delete(created.getFileId()), "delete should return true");
        assertNull(dao.findById(created.getFileId()), "deleted material should not be found");
    }

    @AfterAll
    public static void afterClass() throws SQLException {
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