// java
package com.example.otp.dao;

import com.example.otp.db.Database;
import com.example.otp.model.User;
import com.example.otp.model.Course;
import com.example.otp.model.Material;
import com.example.otp.model.Review;
import org.junit.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.*;

public class ReviewDaoTest {
    private static ReviewDao dao;
    private static UserDao userDao;
    private static CourseDao courseDao;
    private static MaterialDao materialDao;

    @BeforeClass
    public static void beforeClass() throws Exception {
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM reviews");
            s.executeUpdate("DELETE FROM materials");
            s.executeUpdate("DELETE FROM participants");
            s.executeUpdate("DELETE FROM classes");
            s.executeUpdate("DELETE FROM users");
        }
        dao = new ReviewDao();
        userDao = new UserDao();
        courseDao = new CourseDao();
        materialDao = new MaterialDao();
    }

    @Test
    public void testCreateFindUpdateDelete() throws SQLException {
        // prepare user, class, material
        User u = new User();
        String uname = "review_user_" + System.currentTimeMillis();
        u.setUsername(uname);
        u.setPasswordHash("hash");
        u.setUserType("student");
        User createdUser = userDao.create(u);
        assertNotNull(createdUser.getUserId());

        Course c = new Course();
        String cname = "review_class_" + System.currentTimeMillis();
        c.setClassName(cname);
        c.setTopic("Topic");
        Course createdClass = courseDao.create(c);
        assertNotNull(createdClass.getClassId());

        Material m = new Material();
        m.setOriginalFilename("revfile.txt");
        m.setStoredFilename("stored-" + System.currentTimeMillis());
        m.setFilepath("/tmp/rev-" + System.currentTimeMillis() + ".txt");
        m.setMaterialType("TXT");
        m.setClassId(createdClass.getClassId());
        m.setUserId(createdUser.getUserId());
        Material createdMaterial = materialDao.create(m);
        assertNotNull(createdMaterial.getFileId());

        // create review
        Review r = new Review();
        r.setReview("Nice material");
        r.setRating(4);
        r.setFileId(createdMaterial.getFileId());
        r.setUserId(createdUser.getUserId());

        Review created = dao.create(r);
        assertNotNull(created.getReviewId());

        // findById
        Review found = dao.findById(created.getReviewId());
        assertNotNull(found);
        assertEquals((Integer)4, found.getRating());
        assertEquals("Nice material", found.getReview());

        // findByFileId
        List<Review> byFile = dao.findByFileId(createdMaterial.getFileId());
        boolean contains = byFile.stream().anyMatch(x -> x.getReviewId().equals(created.getReviewId()));
        assertTrue("findByFileId should contain created review", contains);

        // update
        created.setReview("Updated review");
        created.setRating(5);
        assertTrue("update should return true", dao.update(created));
        Review updated = dao.findById(created.getReviewId());
        assertNotNull(updated);
        assertEquals((Integer)5, updated.getRating());
        assertEquals("Updated review", updated.getReview());

        // delete
        assertTrue("delete should return true", dao.delete(created.getReviewId()));
        assertNull("deleted review should not be found", dao.findById(created.getReviewId()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRatingRejected() throws SQLException {
        Review r = new Review();
        r.setReview("Bad rating");
        r.setRating(999); // invalid
        // should throw IllegalArgumentException from DAO validation
        dao.create(r);
    }

    @AfterClass
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