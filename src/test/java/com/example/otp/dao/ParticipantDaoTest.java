// java
package com.example.otp.dao;

import com.example.otp.db.Database;
import com.example.otp.model.User;
import com.example.otp.model.Course;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ParticipantDaoTest {
    private static ParticipantDao dao;
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
        dao = new ParticipantDao();
        userDao = new UserDao();
        courseDao = new CourseDao();
    }

    @Test
    public void testAddRemoveAndQueries() throws SQLException {
        // create user
        User u = new User();
        String uname = "participant_user_" + System.currentTimeMillis();
        u.setUsername(uname);
        u.setPasswordHash("hash");
        u.setUserType("student");
        User createdUser = userDao.create(u);
        assertNotNull(createdUser.getUserId());

        // create class
        Course c = new Course();
        String cname = "participant_class_" + System.currentTimeMillis();
        c.setClassName(cname);
        c.setTopic("Topic");
        Course createdClass = courseDao.create(c);
        assertNotNull(createdClass.getClassId());

        // add participant
        assertTrue(dao.addParticipant(createdUser.getUserId(), createdClass.getClassId()), "addParticipant should return true");

        // find users by class
        List<Integer> usersInClass = dao.findUsersByClass(createdClass.getClassId());
        assertTrue(usersInClass.contains(createdUser.getUserId()));

        // find classes by user
        List<Integer> classesForUser = dao.findClassesByUser(createdUser.getUserId());
        assertTrue(classesForUser.contains(createdClass.getClassId()));

        // remove participant
        assertTrue(dao.removeParticipant(createdUser.getUserId(), createdClass.getClassId()), "removeParticipant should return true");

        // verify removal
        usersInClass = dao.findUsersByClass(createdClass.getClassId());
        assertFalse(usersInClass.contains(createdUser.getUserId()));
        classesForUser = dao.findClassesByUser(createdUser.getUserId());
        assertFalse(classesForUser.contains(createdClass.getClassId()));
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