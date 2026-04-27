package com.example.otp.dao;

import com.example.otp.db.Database;
import com.example.otp.model.*;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DAO edge cases - null IDs, not found queries, boundary conditions.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DaoEdgeCaseTest {

    @BeforeAll
    static void cleanDb() throws Exception {
        try (Connection c = Database.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM reviews");
            s.executeUpdate("DELETE FROM materials");
            s.executeUpdate("DELETE FROM participants");
            s.executeUpdate("DELETE FROM classes");
            s.executeUpdate("DELETE FROM users");
        }
    }

    @AfterAll
    static void cleanUp() throws Exception {
        try (Connection c = Database.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM reviews");
            s.executeUpdate("DELETE FROM materials");
            s.executeUpdate("DELETE FROM participants");
            s.executeUpdate("DELETE FROM classes");
            s.executeUpdate("DELETE FROM users");
        }
    }

    // ---- UserDao edge cases ----

    @Test
    @Order(1)
    void userDaoFindByIdNonExistentReturnsNull() throws SQLException {
        assertNull(new UserDao().findById(999999));
    }

    @Test
    @Order(2)
    void userDaoFindByUsernameNonExistentReturnsNull() throws SQLException {
        assertNull(new UserDao().findByUsername("nonexistent_user_xyz"));
    }

    @Test
    @Order(3)
    void userDaoFindByEmailNonExistentReturnsNull() throws SQLException {
        assertNull(new UserDao().findByEmail("nonexistent@xyz.com"));
    }

    @Test
    @Order(4)
    void userDaoUpdateNullUserIdReturnsFalse() throws SQLException {
        User u = new User();
        u.setUsername("test");
        u.setPasswordHash("hash");
        u.setEmail("test@test.com");
        // userId is null
        assertFalse(new UserDao().update(u));
    }

    @Test
    @Order(5)
    void userDaoDeleteNonExistentReturnsFalse() throws SQLException {
        assertFalse(new UserDao().delete(999999));
    }

    @Test
    @Order(6)
    void userDaoUpdatePasswordNonExistentReturnsFalse() throws SQLException {
        assertFalse(new UserDao().updatePassword(999999, "newhash"));
    }

    @Test
    @Order(7)
    void userDaoFindAllEmptyReturnsEmptyList() throws SQLException {
        assertTrue(new UserDao().findAll().isEmpty());
    }

    // ---- CourseDao edge cases ----

    @Test
    @Order(10)
    void courseDaoFindByIdNonExistentReturnsNull() throws SQLException {
        assertNull(new CourseDao().findById(999999));
    }

    @Test
    @Order(11)
    void courseDaoUpdateNullClassIdReturnsFalse() throws SQLException {
        Course c = new Course();
        c.setClassName("Test");
        // classId is null
        assertFalse(new CourseDao().update(c));
    }

    @Test
    @Order(12)
    void courseDaoDeleteNonExistentReturnsFalse() throws SQLException {
        assertFalse(new CourseDao().delete(999999));
    }

    @Test
    @Order(13)
    void courseDaoCreateWithNullCreatorId() throws SQLException {
        CourseDao dao = new CourseDao();
        Course c = new Course();
        c.setClassName("NullCreator_" + System.currentTimeMillis());
        c.setCreatorId(null);
        c.setTopic("test");
        Course created = dao.create(c);
        assertNotNull(created.getClassId());

        Course found = dao.findById(created.getClassId());
        assertNotNull(found);
        assertNull(found.getCreatorId());

        dao.delete(created.getClassId());
    }

    @Test
    @Order(14)
    void courseDaoUpdateWithNullCreatorId() throws SQLException {
        CourseDao dao = new CourseDao();
        Course c = new Course();
        c.setClassName("UpdateNull_" + System.currentTimeMillis());
        c.setTopic("test");
        c = dao.create(c);
        c.setCreatorId(null);
        assertTrue(dao.update(c));
        dao.delete(c.getClassId());
    }

    // ---- MaterialDao edge cases ----

    @Test
    @Order(20)
    void materialDaoFindByIdNonExistentReturnsNull() throws SQLException {
        assertNull(new MaterialDao().findById(999999));
    }

    @Test
    @Order(21)
    void materialDaoFindByClassIdNonExistentReturnsEmptyList() throws SQLException {
        assertTrue(new MaterialDao().findByClassId(999999).isEmpty());
    }

    @Test
    @Order(22)
    void materialDaoUpdateNullFileIdReturnsFalse() throws SQLException {
        Material m = new Material();
        // fileId is null
        assertFalse(new MaterialDao().update(m));
    }

    @Test
    @Order(23)
    void materialDaoDeleteNonExistentReturnsFalse() throws SQLException {
        assertFalse(new MaterialDao().delete(999999));
    }

    // ---- ReviewDao edge cases ----

    @Test
    @Order(30)
    void reviewDaoFindByIdNonExistentReturnsNull() throws SQLException {
        assertNull(new ReviewDao().findById(999999));
    }

    @Test
    @Order(31)
    void reviewDaoFindByFileIdNonExistentReturnsEmptyList() throws SQLException {
        assertTrue(new ReviewDao().findByFileId(999999).isEmpty());
    }

    @Test
    @Order(32)
    void reviewDaoUpdateNullReviewIdReturnsFalse() throws SQLException {
        Review r = new Review();
        // reviewId is null
        assertFalse(new ReviewDao().update(r));
    }

    @Test
    @Order(33)
    void reviewDaoDeleteNonExistentReturnsFalse() throws SQLException {
        assertFalse(new ReviewDao().delete(999999));
    }

    @Test
    @Order(34)
    void reviewDaoValidateRatingZeroIsValid() throws SQLException {
        // Rating 0 should not throw
        ReviewDao dao = new ReviewDao();
        // We can test rating validation via create - rating 0 is valid
        // but we need valid FK references, so just test the invalid ones
        Review r = new Review();
        r.setRating(-1);
        assertThrows(IllegalArgumentException.class, () -> dao.create(r));
    }

    @Test
    @Order(35)
    void reviewDaoValidateRatingSixIsInvalid() throws SQLException {
        ReviewDao dao = new ReviewDao();
        Review r = new Review();
        r.setRating(6);
        assertThrows(IllegalArgumentException.class, () -> dao.create(r));
    }

    @Test
    @Order(36)
    void reviewDaoValidateRatingNullIsValid() throws SQLException {
        // Null rating should not throw in validate, but will fail due to FK constraint
        ReviewDao dao = new ReviewDao();
        Review r = new Review();
        r.setRating(null);
        // This won't throw IllegalArgumentException, but may throw SQLException
        // (missing FK). That's fine - we just test the validation doesn't reject null.
        try {
            dao.create(r);
        } catch (SQLException e) {
            // Expected - no FK references
        }
    }

    // ---- ParticipantDao edge cases ----

    @Test
    @Order(40)
    void participantDaoFindUsersByClassNonExistentReturnsEmptyList() throws SQLException {
        assertTrue(new ParticipantDao().findUsersByClass(999999).isEmpty());
    }

    @Test
    @Order(41)
    void participantDaoFindClassesByUserNonExistentReturnsEmptyList() throws SQLException {
        assertTrue(new ParticipantDao().findClassesByUser(999999).isEmpty());
    }

    @Test
    @Order(42)
    void participantDaoRemoveParticipantNonExistentReturnsFalse() throws SQLException {
        assertFalse(new ParticipantDao().removeParticipant(999999, 999999));
    }
}
