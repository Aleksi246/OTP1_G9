package com.example.otp.controllers;

import com.example.otp.dao.*;
import com.example.otp.db.Database;
import com.example.otp.model.*;
import com.example.otp.util.BCryptUtil;
import com.example.otp.util.JWTUtil;
import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Targets specific uncovered branches in controllers to maximize line coverage.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ControllerBranchCoverageTest {

    private static Javalin app;
    private static String baseUrl;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static int teacherId;
    private static int studentId;
    private static int classId;
    private static int materialId;
    private static int reviewId;
    private static String teacherToken;
    private static String studentToken;
    private static String teacherEmail = "branch_teacher@test.com";
    private static String studentEmail = "branch_student@test.com";

    @BeforeAll
    static void startServer() throws Exception {
        UserController userController = new UserController();
        CourseController courseController = new CourseController();
        MaterialController materialController = new MaterialController();
        ReviewController reviewController = new ReviewController();
        ParticipantController participantController = new ParticipantController();

        app = Javalin.create(config ->
                config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost())));

        // Mirror the production auth middleware from Main.java
        String[] protectedPaths = {
                "/api/users", "/api/users/*",
                "/api/courses", "/api/courses/*",
                "/api/materials", "/api/materials/*",
                "/api/participants/unenroll", "/api/participants/class/*", "/api/participants/user/*",
                "/api/auth/change-password"
        };
        for (String path : protectedPaths) {
            app.before(path, ctx -> checkAuth(ctx));
        }

        app.post("/api/auth/register", userController::register);
        app.post("/api/auth/login", userController::login);
        app.put("/api/auth/change-password", userController::changePassword);
        app.get("/api/users", userController::getAllUsers);
        app.get("/api/users/{id}", userController::getUserById);
        app.get("/api/users/by-email/{email}", userController::getUserByEmail);
        app.get("/api/courses", courseController::getAllCourses);
        app.get("/api/courses/{id}", courseController::getCourseById);
        app.post("/api/courses", courseController::createCourse);
        app.put("/api/courses/{id}", courseController::updateCourse);
        app.delete("/api/courses/{id}", courseController::deleteCourse);
        app.get("/api/materials/course/{classId}", materialController::getMaterialsByCourse);
        app.get("/api/materials/{id}", materialController::getMaterialById);
        app.get("/api/materials/{id}/download", materialController::downloadMaterial);
        app.post("/api/materials", materialController::uploadMaterial);
        app.put("/api/materials/{id}", materialController::updateMaterial);
        app.delete("/api/materials/{id}", materialController::deleteMaterial);
        app.get("/api/reviews/material/{fileId}", reviewController::getReviewsByMaterial);
        app.get("/api/reviews/{id}", reviewController::getReviewById);
        app.post("/api/reviews", reviewController::createReview);
        app.put("/api/reviews/{id}", reviewController::updateReview);
        app.delete("/api/reviews/{id}", reviewController::deleteReview);
        app.post("/api/participants/enroll", participantController::enrollUser);
        app.delete("/api/participants/unenroll", participantController::unenrollUser);
        app.get("/api/participants/class/{classId}", participantController::getUsersByClass);
        app.get("/api/participants/user/{userId}", participantController::getClassesByUser);

        app.start(0);
        baseUrl = "http://localhost:" + app.port();
        seedData();
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (app != null) app.stop();
        clearAllTables();
    }

    private static void seedData() throws Exception {
        clearAllTables();
        UserDao userDao = new UserDao();
        CourseDao courseDao = new CourseDao();
        ParticipantDao participantDao = new ParticipantDao();
        MaterialDao materialDao = new MaterialDao();
        ReviewDao reviewDao = new ReviewDao();

        User teacher = createUser(userDao, "branch_teacher", teacherEmail, "pass123");
        User student = createUser(userDao, "branch_student", studentEmail, "pass123");
        teacherId = teacher.getUserId();
        studentId = student.getUserId();
        teacherToken = JWTUtil.generateToken(teacher.getEmail());
        studentToken = JWTUtil.generateToken(student.getEmail());

        Course c = new Course();
        c.setClassName("Branch Test Class");
        c.setCreatorId(teacherId);
        c.setTopic("Coverage");
        c = courseDao.create(c);
        classId = c.getClassId();

        participantDao.addParticipant(teacherId, classId);
        participantDao.addParticipant(studentId, classId);

        Files.createDirectories(Path.of("uploads"));
        Files.writeString(Path.of("uploads/branch_stored.txt"), "branch test content");

        Material m = new Material();
        m.setOriginalFilename("branch_test.txt");
        m.setStoredFilename("branch_stored.txt");
        m.setFilepath("uploads/branch_stored.txt");
        m.setMaterialType("Slides");
        m.setClassId(classId);
        m.setUserId(teacherId);
        m = materialDao.create(m);
        materialId = m.getFileId();

        Review r = new Review();
        r.setReview("Branch test review");
        r.setRating(4);
        r.setFileId(materialId);
        r.setUserId(studentId);
        r = reviewDao.create(r);
        reviewId = r.getReviewId();
    }

    // ---- UserController: register duplicate username ----
    @Test
    @Order(1)
    void register_duplicateUsername_returns409() throws Exception {
        var resp = sendJson("POST", "/api/auth/register", null, """
                {"username":"branch_teacher","email":"new@test.com","password":"pass123"}""");
        assertEquals(409, resp.statusCode());
        assertTrue(resp.body().contains("Username already exists"));
    }

    // ---- UserController: register duplicate email ----
    @Test
    @Order(2)
    void register_duplicateEmail_returns409() throws Exception {
        var resp = sendJson("POST", "/api/auth/register", null, """
                {"username":"new_user","email":"branch_teacher@test.com","password":"pass123"}""");
        assertEquals(409, resp.statusCode());
        assertTrue(resp.body().contains("Email already exists"));
    }

    // ---- UserController: login with wrong password ----
    @Test
    @Order(3)
    void login_wrongPassword_returns401() throws Exception {
        var resp = sendJson("POST", "/api/auth/login", null, """
                {"email":"branch_teacher@test.com","password":"wrongpass"}""");
        assertEquals(401, resp.statusCode());
        assertTrue(resp.body().contains("Invalid credentials"));
    }

    // ---- UserController: login by username ----
    @Test
    @Order(4)
    void login_byUsername_returns200() throws Exception {
        var resp = sendJson("POST", "/api/auth/login", null, """
                {"username":"branch_teacher","password":"pass123"}""");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("token"));
    }

    // ---- UserController: login by username that looks like email (contains @) ----
    @Test
    @Order(5)
    void login_usernameWithAtSign_fallbackToEmailLookup() throws Exception {
        var resp = sendJson("POST", "/api/auth/login", null, """
                {"username":"branch_teacher@test.com","password":"pass123"}""");
        assertEquals(200, resp.statusCode());
    }

    // ---- UserController: getUserByEmail - found ----
    @Test
    @Order(6)
    void getUserByEmail_found_returns200() throws Exception {
        var resp = sendJson("GET", "/api/users/by-email/" + teacherEmail, bearer(teacherToken), null);
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("branch_teacher"));
    }

    // ---- UserController: getUserByEmail - not found ----
    @Test
    @Order(7)
    void getUserByEmail_notFound_returns404() throws Exception {
        var resp = sendJson("GET", "/api/users/by-email/nonexistent@x.com", bearer(teacherToken), null);
        assertEquals(404, resp.statusCode());
    }

    // ---- UserController: getUserById - not found ----
    @Test
    @Order(8)
    void getUserById_notFound_returns404() throws Exception {
        var resp = sendJson("GET", "/api/users/999999", bearer(teacherToken), null);
        assertEquals(404, resp.statusCode());
    }

    // ---- UserController: changePassword - wrong current password ----
    @Test
    @Order(9)
    void changePassword_wrongCurrentPassword_returns401() throws Exception {
        var resp = sendJson("PUT", "/api/auth/change-password", bearer(teacherToken), """
                {"currentPassword":"wrong","newPassword":"newpass123"}""");
        assertEquals(401, resp.statusCode());
        assertTrue(resp.body().contains("Current password is incorrect"));
    }

    // ---- UserController: changePassword - missing fields ----
    @Test
    @Order(10)
    void changePassword_missingFields_returns400() throws Exception {
        var resp = sendJson("PUT", "/api/auth/change-password", bearer(teacherToken), """
                {"currentPassword":"","newPassword":""}""");
        assertEquals(400, resp.statusCode());
    }

    // ---- UserController: changePassword - success ----
    @Test
    @Order(11)
    void changePassword_success_returns200() throws Exception {
        var resp = sendJson("PUT", "/api/auth/change-password", bearer(studentToken), """
                {"currentPassword":"pass123","newPassword":"newpass456"}""");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("Password changed successfully"));

        // Change back so other tests still work
        String newToken = JWTUtil.generateToken(studentEmail);
        var resp2 = sendJson("PUT", "/api/auth/change-password", bearer(newToken), """
                {"currentPassword":"newpass456","newPassword":"pass123"}""");
        assertEquals(200, resp2.statusCode());
    }

    // ---- CourseController: getAllCourses ----
    @Test
    @Order(15)
    void getAllCourses_returns200() throws Exception {
        var resp = sendJson("GET", "/api/courses", bearer(teacherToken), null);
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("Branch Test Class"));
    }

    // ---- CourseController: getCourseById found ----
    @Test
    @Order(16)
    void getCourseById_found_returns200() throws Exception {
        var resp = sendJson("GET", "/api/courses/" + classId, bearer(teacherToken), null);
        assertEquals(200, resp.statusCode());
    }

    // ---- CourseController: getCourseById not found ----
    @Test
    @Order(17)
    void getCourseById_notFound_returns404() throws Exception {
        var resp = sendJson("GET", "/api/courses/999999", bearer(teacherToken), null);
        assertEquals(404, resp.statusCode());
    }

    // ---- CourseController: updateCourse by creator - success ----
    @Test
    @Order(18)
    void updateCourse_byCreator_success() throws Exception {
        var resp = sendJson("PUT", "/api/courses/" + classId, bearer(teacherToken), """
                {"className":"Updated Branch Class","topic":"Updated Topic"}""");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("Updated Branch Class"));
    }

    // ---- CourseController: updateCourse - course not found ----
    @Test
    @Order(19)
    void updateCourse_courseNotFound_returns404() throws Exception {
        var resp = sendJson("PUT", "/api/courses/999999", bearer(teacherToken), """
                {"className":"x"}""");
        assertEquals(404, resp.statusCode());
    }

    // ---- CourseController: updateCourse - non-creator ----
    @Test
    @Order(20)
    void updateCourse_nonCreator_returns403() throws Exception {
        var resp = sendJson("PUT", "/api/courses/" + classId, bearer(studentToken), """
                {"className":"Stolen!"}""");
        assertEquals(403, resp.statusCode());
    }

    // ---- CourseController: deleteCourse by non-creator ----
    @Test
    @Order(21)
    void deleteCourse_nonCreator_returns403() throws Exception {
        var resp = sendJson("DELETE", "/api/courses/" + classId, bearer(studentToken), null);
        assertEquals(403, resp.statusCode());
    }

    // ---- CourseController: deleteCourse not found ----
    @Test
    @Order(22)
    void deleteCourse_notFound_returns404() throws Exception {
        var resp = sendJson("DELETE", "/api/courses/999999", bearer(teacherToken), null);
        assertEquals(404, resp.statusCode());
    }

    // ---- MaterialController: getMaterialById found ----
    @Test
    @Order(30)
    void getMaterialById_found_returns200() throws Exception {
        var resp = sendJson("GET", "/api/materials/" + materialId, bearer(teacherToken), null);
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("branch_test.txt"));
    }

    // ---- MaterialController: downloadMaterial by enrolled user ----
    @Test
    @Order(31)
    void downloadMaterial_enrolledUser_returnsFile() throws Exception {
        var resp = sendJson("GET", "/api/materials/" + materialId + "/download", bearer(studentToken), null);
        assertEquals(200, resp.statusCode());
    }

    // ---- MaterialController: downloadMaterial by unenrolled user ----
    @Test
    @Order(32)
    void downloadMaterial_unenrolledUser_returns403() throws Exception {
        // Create a new user not enrolled in the class
        UserDao userDao = new UserDao();
        User outsider = createUser(userDao, "outsider_" + System.currentTimeMillis(), "outsider@branch.com", "pass");
        String outsiderToken = JWTUtil.generateToken(outsider.getEmail());

        var resp = sendJson("GET", "/api/materials/" + materialId + "/download", bearer(outsiderToken), null);
        assertEquals(403, resp.statusCode());

        userDao.delete(outsider.getUserId());
    }

    // ---- MaterialController: updateMaterial with JSON body ----
    @Test
    @Order(33)
    void updateMaterial_withJsonBody_updatesFields() throws Exception {
        var resp = sendJson("PUT", "/api/materials/" + materialId, bearer(teacherToken), """
                {"originalFilename":"renamed.txt","materialType":"Reference"}""");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("renamed.txt"));
    }

    // ---- MaterialController: updateMaterial by non-creator ----
    @Test
    @Order(34)
    void updateMaterial_nonCreator_returns403() throws Exception {
        var resp = sendJson("PUT", "/api/materials/" + materialId, bearer(studentToken), """
                {"originalFilename":"stolen.txt"}""");
        assertEquals(403, resp.statusCode());
    }

    // ---- MaterialController: updateMaterial not found ----
    @Test
    @Order(35)
    void updateMaterial_notFound_returns404() throws Exception {
        var resp = sendJson("PUT", "/api/materials/999999", bearer(teacherToken), """
                {"originalFilename":"x.txt"}""");
        assertEquals(404, resp.statusCode());
    }

    // ---- MaterialController: deleteMaterial not found ----
    @Test
    @Order(36)
    void deleteMaterial_notFound_returns404() throws Exception {
        var resp = sendJson("DELETE", "/api/materials/999999", bearer(teacherToken), null);
        assertEquals(404, resp.statusCode());
    }

    // ---- ReviewController: getReviewsByMaterial ----
    @Test
    @Order(40)
    void getReviewsByMaterial_returns200() throws Exception {
        var resp = sendJson("GET", "/api/reviews/material/" + materialId, null, null);
        assertEquals(200, resp.statusCode());
    }

    // ---- ReviewController: getReviewById found ----
    @Test
    @Order(41)
    void getReviewById_found_returns200() throws Exception {
        var resp = sendJson("GET", "/api/reviews/" + reviewId, null, null);
        assertEquals(200, resp.statusCode());
    }

    // ---- ReviewController: getReviewById not found ----
    @Test
    @Order(42)
    void getReviewById_notFound_returns404() throws Exception {
        var resp = sendJson("GET", "/api/reviews/999999", null, null);
        assertEquals(404, resp.statusCode());
    }

    // ---- ReviewController: createReview with auth header (not attribute) ----
    @Test
    @Order(43)
    void createReview_withBearerToken_returns201() throws Exception {
        // This tests the getAuthenticatedUser fallback when email attribute isn't set
        var resp = sendJson("POST", "/api/reviews", bearer(studentToken), """
                {"review":"Coverage test review","rating":3,"fileId":%d}""".formatted(materialId));
        assertTrue(resp.statusCode() == 201 || resp.statusCode() == 200);
    }

    // ---- ReviewController: createReview missing review text ----
    @Test
    @Order(44)
    void createReview_missingReviewText_returns400() throws Exception {
        var resp = sendJson("POST", "/api/reviews", bearer(studentToken), """
                {"rating":3,"fileId":%d}""".formatted(materialId));
        assertEquals(400, resp.statusCode());
    }

    // ---- ReviewController: updateReview - not owner ----
    @Test
    @Order(45)
    void updateReview_notOwner_returns403() throws Exception {
        var resp = sendJson("PUT", "/api/reviews/" + reviewId, bearer(teacherToken), """
                {"review":"hijacked","rating":1}""");
        assertEquals(403, resp.statusCode());
    }

    // ---- ReviewController: updateReview - success ----
    @Test
    @Order(46)
    void updateReview_byOwner_returns200() throws Exception {
        var resp = sendJson("PUT", "/api/reviews/" + reviewId, bearer(studentToken), """
                {"review":"Updated review","rating":5}""");
        assertEquals(200, resp.statusCode());
    }

    // ---- ReviewController: deleteReview - not owner ----
    @Test
    @Order(47)
    void deleteReview_notOwner_returns403() throws Exception {
        var resp = sendJson("DELETE", "/api/reviews/" + reviewId, bearer(teacherToken), null);
        assertEquals(403, resp.statusCode());
    }

    // ---- ReviewController: createReview with invalid token ----
    @Test
    @Order(48)
    void createReview_invalidToken_returns401() throws Exception {
        var resp = sendJson("POST", "/api/reviews", "Bearer invalidtoken123", """
                {"review":"test","rating":3,"fileId":%d}""".formatted(materialId));
        // The ReviewController.getAuthenticatedUser should return null
        assertEquals(401, resp.statusCode());
    }

    // ---- ReviewController: createReview with no auth header ----
    @Test
    @Order(49)
    void createReview_noAuth_returns401() throws Exception {
        var resp = sendJson("POST", "/api/reviews", null, """
                {"review":"test","rating":3,"fileId":%d}""".formatted(materialId));
        assertEquals(401, resp.statusCode());
    }

    // ---- ParticipantController: enrollUser - user not found ----
    @Test
    @Order(50)
    void enrollUser_userNotFound_returns400() throws Exception {
        var resp = sendJson("POST", "/api/participants/enroll", null, """
                {"userId":999999,"classId":%d}""".formatted(classId));
        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("User to enroll does not exist"));
    }

    // ---- ParticipantController: enrollUser - class not found ----
    @Test
    @Order(51)
    void enrollUser_classNotFound_returns400() throws Exception {
        var resp = sendJson("POST", "/api/participants/enroll", null, """
                {"userId":%d,"classId":999999}""".formatted(teacherId));
        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Class does not exist"));
    }

    // ---- ParticipantController: unenroll non-self non-creator ----
    @Test
    @Order(52)
    void unenroll_nonSelfNonCreator_returns403() throws Exception {
        // Create a third user enrolled in the class
        UserDao userDao = new UserDao();
        User third = createUser(userDao, "third_" + System.currentTimeMillis(), "third@branch.com", "pass");
        new ParticipantDao().addParticipant(third.getUserId(), classId);
        String thirdToken = JWTUtil.generateToken(third.getEmail());

        // third user tries to unenroll the teacher (not self, not creator)
        var resp = sendJson("DELETE", "/api/participants/unenroll", bearer(thirdToken), """
                {"userId":%d,"classId":%d}""".formatted(teacherId, classId));
        assertEquals(403, resp.statusCode());

        new ParticipantDao().removeParticipant(third.getUserId(), classId);
        userDao.delete(third.getUserId());
    }

    // ---- ParticipantController: unenroll enrollment not found ----
    @Test
    @Order(53)
    void unenroll_enrollmentNotFound_returns404() throws Exception {
        // Self-unenroll from a class you're not actually enrolled in
        UserDao userDao = new UserDao();
        User user = createUser(userDao, "unenroll_" + System.currentTimeMillis(), "unenroll@branch.com", "pass");
        String token = JWTUtil.generateToken(user.getEmail());

        var resp = sendJson("DELETE", "/api/participants/unenroll", bearer(token), """
                {"userId":%d,"classId":%d}""".formatted(user.getUserId(), classId));
        assertEquals(404, resp.statusCode());

        userDao.delete(user.getUserId());
    }

    // ---- MaterialController: downloadMaterial not found ----
    @Test
    @Order(54)
    void downloadMaterial_materialNotFound_returns404() throws Exception {
        var resp = sendJson("GET", "/api/materials/999999/download", bearer(teacherToken), null);
        assertEquals(404, resp.statusCode());
    }

    // ---- UserController: login with email field (primary path) ----
    @Test
    @Order(55)
    void login_byEmail_returns200() throws Exception {
        var resp = sendJson("POST", "/api/auth/login", null, """
                {"email":"branch_teacher@test.com","password":"pass123"}""");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("token"));
    }

    // ---- UserController: login with empty string email and empty username ----
    @Test
    @Order(56)
    void login_blankEmailAndUsername_returns400() throws Exception {
        var resp = sendJson("POST", "/api/auth/login", null, """
                {"email":"","username":"","password":"pass123"}""");
        assertEquals(400, resp.statusCode());
    }

    // ---- UserController: login blank password ----
    @Test
    @Order(57)
    void login_blankPassword_returns400() throws Exception {
        var resp = sendJson("POST", "/api/auth/login", null, """
                {"email":"branch_teacher@test.com","password":""}""");
        assertEquals(400, resp.statusCode());
    }

    // ---- MaterialController: getMaterialsByCourse ----
    @Test
    @Order(60)
    void getMaterialsByCourse_returns200() throws Exception {
        var resp = sendJson("GET", "/api/materials/course/" + classId, bearer(teacherToken), null);
        assertEquals(200, resp.statusCode());
    }

    // ---- Helpers ----

    private static void checkAuth(io.javalin.http.Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Missing token");
        }
        String email = JWTUtil.validateToken(authHeader.substring(7));
        if (email == null) throw new UnauthorizedResponse("Invalid token");
        ctx.attribute("email", email);
    }

    private static void clearAllTables() throws Exception {
        try (Connection c = Database.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM reviews");
            s.executeUpdate("DELETE FROM materials");
            s.executeUpdate("DELETE FROM participants");
            s.executeUpdate("DELETE FROM classes");
            s.executeUpdate("DELETE FROM users");
        }
    }

    private static User createUser(UserDao dao, String username, String email, String password) throws Exception {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(BCryptUtil.hashPassword(password));
        return dao.create(user);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static HttpResponse<String> sendJson(String method, String path, String authorization, String body)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(baseUrl + path));
        if (authorization != null) builder.header("Authorization", authorization);
        if (body != null) builder.header("Content-Type", "application/json");
        switch (method) {
            case "POST" -> builder.POST(body == null ? HttpRequest.BodyPublishers.noBody() :
                    HttpRequest.BodyPublishers.ofString(body));
            case "PUT" -> builder.PUT(body == null ? HttpRequest.BodyPublishers.noBody() :
                    HttpRequest.BodyPublishers.ofString(body));
            case "DELETE" -> {
                if (body == null) builder.DELETE();
                else builder.method("DELETE", HttpRequest.BodyPublishers.ofString(body));
            }
            case "GET" -> builder.GET();
            default -> throw new IllegalArgumentException("Unsupported: " + method);
        }
        return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
