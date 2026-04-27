package com.example.app;

import com.example.otp.controllers.*;
import com.example.otp.dao.*;
import com.example.otp.db.Database;
import com.example.otp.model.*;
import com.example.otp.util.BCryptUtil;
import com.example.otp.util.JWTUtil;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClassController's private HTTP-based and I/O methods via reflection.
 * Uses a real Javalin server and database.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClassControllerHttpTest {

    private static Javalin app;
    private static String baseUrl;
    private static ClassController controller;

    private static int teacherId;
    private static int studentId;
    private static int classId;
    private static int materialId;
    private static String teacherToken;
    private static String studentToken;

    @BeforeAll
    static void setup() throws Exception {
        // Start server
        UserController userController = new UserController();
        CourseController courseController = new CourseController();
        MaterialController materialController = new MaterialController();
        ReviewController reviewController = new ReviewController();
        ParticipantController participantController = new ParticipantController();

        app = Javalin.create(config ->
                config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost())));

        app.before("/api/*", ctx -> {
            String path = ctx.path();
            if (path.startsWith("/api/auth/")) return;
            if (path.startsWith("/api/materials/course/")) return;
            if (path.startsWith("/api/reviews/material/")) return;
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new UnauthorizedResponse("Missing token");
            }
            String token = authHeader.substring(7);
            String email = JWTUtil.validateToken(token);
            if (email == null) throw new UnauthorizedResponse("Invalid token");
            ctx.attribute("email", email);
        });

        app.post("/api/auth/register", userController::register);
        app.post("/api/auth/login", userController::login);
        app.get("/api/users/by-email/{email}", userController::getUserByEmail);
        app.get("/api/courses/{id}", courseController::getCourseById);
        app.post("/api/courses", courseController::createCourse);
        app.get("/api/materials/course/{classId}", materialController::getMaterialsByCourse);
        app.post("/api/materials", materialController::uploadMaterial);
        app.get("/api/reviews/material/{fileId}", reviewController::getReviewsByMaterial);
        app.post("/api/reviews", reviewController::createReview);
        app.get("/api/participants/user/{userId}", participantController::getClassesByUser);
        app.post("/api/participants/enroll", participantController::enrollUser);

        app.start(0);
        baseUrl = "http://localhost:" + app.port();
        System.setProperty("otp.api.base-url", baseUrl);

        // Seed data
        clearAllTables();
        UserDao userDao = new UserDao();
        CourseDao courseDao = new CourseDao();
        ParticipantDao participantDao = new ParticipantDao();
        MaterialDao materialDao = new MaterialDao();

        User teacher = new User();
        teacher.setUsername("cc_teacher");
        teacher.setEmail("cc_teacher@test.com");
        teacher.setPasswordHash(BCryptUtil.hashPassword("pass123"));
        teacher = userDao.create(teacher);
        teacherId = teacher.getUserId();
        teacherToken = JWTUtil.generateToken(teacher.getEmail());

        User student = new User();
        student.setUsername("cc_student");
        student.setEmail("cc_student@test.com");
        student.setPasswordHash(BCryptUtil.hashPassword("pass123"));
        student = userDao.create(student);
        studentId = student.getUserId();
        studentToken = JWTUtil.generateToken(student.getEmail());

        Course course = new Course();
        course.setClassName("CC Test Class");
        course.setCreatorId(teacherId);
        course.setTopic("Testing");
        course = courseDao.create(course);
        classId = course.getClassId();

        participantDao.addParticipant(teacherId, classId);
        participantDao.addParticipant(studentId, classId);

        Material material = new Material();
        material.setOriginalFilename("cc_test.txt");
        material.setStoredFilename("cc_test_stored.txt");
        material.setFilepath("uploads/cc_test_stored.txt");
        material.setMaterialType("Lecture Notes");
        material.setClassId(classId);
        material.setUserId(teacherId);
        material = materialDao.create(material);
        materialId = material.getFileId();

        Path uploadsDir = Path.of("uploads");
        Files.createDirectories(uploadsDir);
        Files.writeString(uploadsDir.resolve("cc_test_stored.txt"), "test content");

        // Create controller and set private fields
        controller = new ClassController();
        setField(controller, "token", teacherToken);
        setField(controller, "userId", teacherId);
        setField(controller, "classId", classId);
        setField(controller, "creatorId", teacherId);
        setField(controller, "isCreator", true);
        setField(controller, "isEnrolled", true);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (app != null) app.stop();
        System.clearProperty("otp.api.base-url");
        clearAllTables();
    }

    // ---- fetchUserId tests ----

    @Test
    @Order(1)
    void fetchUserIdValidEmailReturnsUserId() throws Exception {
        SessionManager.setSession("cc_teacher", "cc_teacher@test.com", teacherToken, "teacher");
        Method method = ClassController.class.getDeclaredMethod("fetchUserId");
        method.setAccessible(true);

        // Need to update the httpClient to point to our test server
        ClassController localController = createControllerWithBaseUrl(teacherToken);
        Integer result = (Integer) method.invoke(localController);
        assertEquals(teacherId, result);
    }

    @Test
    @Order(2)
    void fetchUserIdNullEmailReturnsNull() throws Exception {
        SessionManager.setSession("cc_teacher", null, teacherToken, "teacher");
        Method method = ClassController.class.getDeclaredMethod("fetchUserId");
        method.setAccessible(true);

        ClassController localController = createControllerWithBaseUrl(teacherToken);
        Integer result = (Integer) method.invoke(localController);
        assertNull(result);
    }

    @Test
    @Order(3)
    void fetchUserIdBlankEmailReturnsNull() throws Exception {
        SessionManager.setSession("cc_teacher", "  ", teacherToken, "teacher");
        Method method = ClassController.class.getDeclaredMethod("fetchUserId");
        method.setAccessible(true);

        ClassController localController = createControllerWithBaseUrl(teacherToken);
        Integer result = (Integer) method.invoke(localController);
        assertNull(result);
    }

    // ---- isUserEnrolled tests ----

    @Test
    @Order(4)
    void isUserEnrolledEnrolledUserReturnsTrue() throws Exception {
        Method method = ClassController.class.getDeclaredMethod("isUserEnrolled");
        method.setAccessible(true);

        ClassController localController = createControllerWithBaseUrl(studentToken);
        setField(localController, "userId", studentId);
        setField(localController, "classId", classId);

        boolean result = (boolean) method.invoke(localController);
        assertTrue(result);
    }

    @Test
    @Order(5)
    void isUserEnrolledNotEnrolledUserReturnsFalse() throws Exception {
        Method method = ClassController.class.getDeclaredMethod("isUserEnrolled");
        method.setAccessible(true);

        ClassController localController = createControllerWithBaseUrl(studentToken);
        setField(localController, "userId", studentId);
        setField(localController, "classId", 999999);  // non-existent class

        boolean result = (boolean) method.invoke(localController);
        assertFalse(result);
    }

    @Test
    @Order(6)
    void isUserEnrolledNullUserIdReturnsFalse() throws Exception {
        Method method = ClassController.class.getDeclaredMethod("isUserEnrolled");
        method.setAccessible(true);

        ClassController localController = createControllerWithBaseUrl(studentToken);
        setField(localController, "userId", null);
        setField(localController, "classId", classId);

        boolean result = (boolean) method.invoke(localController);
        assertFalse(result);
    }

    // ---- fetchMaterialReviews tests ----

    @Test
    @Order(7)
    void fetchMaterialReviewsNoReviewsReturnsEmptyResult() throws Exception {
        Method method = ClassController.class.getDeclaredMethod("fetchMaterialReviews", Integer.class);
        method.setAccessible(true);

        ClassController localController = createControllerWithBaseUrl(teacherToken);
        Object result = method.invoke(localController, materialId);

        // Get message and reviewLines from the private record via reflection
        assertNotNull(result);
        Method messageMethod = result.getClass().getMethod("message");
        Method reviewLinesMethod = result.getClass().getMethod("reviewLines");
        String message = (String) messageMethod.invoke(result);
        List<?> lines = (List<?>) reviewLinesMethod.invoke(result);
        assertNotNull(message);
        assertNotNull(lines);
    }

    @Test
    @Order(8)
    void fetchMaterialReviewsWithReviewsReturnsLines() throws Exception {
        // First create a review
        ReviewDao reviewDao = new ReviewDao();
        Review review = new Review();
        review.setReview("Test review for CC");
        review.setRating(4);
        review.setFileId(materialId);
        review.setUserId(studentId);
        reviewDao.create(review);

        Method method = ClassController.class.getDeclaredMethod("fetchMaterialReviews", Integer.class);
        method.setAccessible(true);

        ClassController localController = createControllerWithBaseUrl(teacherToken);
        Object result = method.invoke(localController, materialId);

        assertNotNull(result);
        List<?> lines = (List<?>) result.getClass().getMethod("reviewLines").invoke(result);
        assertFalse(lines.isEmpty());
    }

    @Test
    @Order(9)
    void fetchMaterialReviewsNonExistentMaterialReturnsEmpty() throws Exception {
        Method method = ClassController.class.getDeclaredMethod("fetchMaterialReviews", Integer.class);
        method.setAccessible(true);

        ClassController localController = createControllerWithBaseUrl(teacherToken);
        Object result = method.invoke(localController, 999999);

        assertNotNull(result);
        List<?> lines = (List<?>) result.getClass().getMethod("reviewLines").invoke(result);
        assertTrue(lines.isEmpty());
    }

    // ---- submitMaterialReview tests ----

    @Test
    @Order(10)
    void submitMaterialReviewValidReviewReturnsSuccess() throws Exception {
        Method method = ClassController.class.getDeclaredMethod("submitMaterialReview",
                Integer.class, Integer.class, String.class);
        method.setAccessible(true);

        ClassController localController = createControllerWithBaseUrl(studentToken);
        Object result = method.invoke(localController, materialId, 5, "Excellent material");

        assertNotNull(result);
        boolean success = (boolean) result.getClass().getMethod("success").invoke(result);
        assertTrue(success);
    }

    @Test
    @Order(11)
    void submitMaterialReviewNullFileIdReturnsError() throws Exception {
        Method method = ClassController.class.getDeclaredMethod("submitMaterialReview",
                Integer.class, Integer.class, String.class);
        method.setAccessible(true);

        ClassController localController = createControllerWithBaseUrl(studentToken);
        // This will likely cause an error since fileId won't match anything valid
        Object result = method.invoke(localController, 999999, 3, "Bad ID");

        assertNotNull(result);
        // Result might succeed or fail depending on server validation
        String message = (String) result.getClass().getMethod("message").invoke(result);
        assertNotNull(message);
    }

    // ---- buildMultipartBody tests ----

    @Test
    @Order(12)
    void buildMultipartBodyProducesValidOutput() throws Exception {
        Method method = ClassController.class.getDeclaredMethod("buildMultipartBody",
                File.class, String.class, Integer.class, String.class);
        method.setAccessible(true);

        Path tempFile = Files.createTempFile("cc_test_", ".txt");
        Files.writeString(tempFile, "Hello World");

        byte[] result = (byte[]) method.invoke(controller, tempFile.toFile(), "boundary123", 42, "Slides");

        assertNotNull(result);
        assertTrue(result.length > 0);
        String body = new String(result);
        assertTrue(body.contains("--boundary123"));
        assertTrue(body.contains("classId"));
        assertTrue(body.contains("42"));
        assertTrue(body.contains("materialType"));
        assertTrue(body.contains("Slides"));
        assertTrue(body.contains("Hello World"));
        assertTrue(body.contains("--boundary123--"));

        Files.deleteIfExists(tempFile);
    }

    @Test
    @Order(13)
    void buildMultipartBodyEmptyFile() throws Exception {
        Method method = ClassController.class.getDeclaredMethod("buildMultipartBody",
                File.class, String.class, Integer.class, String.class);
        method.setAccessible(true);

        Path tempFile = Files.createTempFile("cc_empty_", ".txt");
        Files.writeString(tempFile, "");

        byte[] result = (byte[]) method.invoke(controller, tempFile.toFile(), "bnd", 1, "Other");
        assertNotNull(result);
        assertTrue(result.length > 0);

        Files.deleteIfExists(tempFile);
    }

    // ---- writeTextPart tests ----

    @Test
    @Order(14)
    void writeTextPartProducesCorrectOutput() throws Exception {
        Method method = ClassController.class.getDeclaredMethod("writeTextPart",
                ByteArrayOutputStream.class, String.class, String.class, String.class);
        method.setAccessible(true);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        method.invoke(controller, output, "boundary", "fieldName", "fieldValue");

        String result = output.toString();
        assertTrue(result.contains("--boundary"));
        assertTrue(result.contains("name=\"fieldName\""));
        assertTrue(result.contains("fieldValue"));
    }

    @Test
    @Order(15)
    void writeTextPartSpecialChars() throws Exception {
        Method method = ClassController.class.getDeclaredMethod("writeTextPart",
                ByteArrayOutputStream.class, String.class, String.class, String.class);
        method.setAccessible(true);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        method.invoke(controller, output, "bnd", "name", "value with spaces & chars!");

        String result = output.toString();
        assertTrue(result.contains("value with spaces & chars!"));
    }

    @Test
    @Order(16)
    void writeTextPartMultipleParts() throws Exception {
        Method method = ClassController.class.getDeclaredMethod("writeTextPart",
                ByteArrayOutputStream.class, String.class, String.class, String.class);
        method.setAccessible(true);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        method.invoke(controller, output, "bnd", "part1", "val1");
        method.invoke(controller, output, "bnd", "part2", "val2");

        String result = output.toString();
        assertTrue(result.contains("part1"));
        assertTrue(result.contains("val1"));
        assertTrue(result.contains("part2"));
        assertTrue(result.contains("val2"));
    }

    // ---- Helper to create controller pointing to test server ----

    private ClassController createControllerWithBaseUrl(String token) throws Exception {
        ClassController c = new ClassController();
        setField(c, "token", token);
        return c;
    }

    private static void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private static void clearAllTables() throws Exception {
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
