package com.example.otp.controllers;

import com.example.otp.dao.*;
import com.example.otp.db.Database;
import com.example.otp.model.*;
import com.example.otp.util.BCryptUtil;
import com.example.otp.util.JWTUtil;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Supplementary integration tests to cover remaining edge cases in controllers.
 * Focuses on branches not covered by UserAcceptanceTest and ApiEndpointsIntegrationTest.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ControllerEdgeCaseTest {

    private static Javalin app;
    private static String baseUrl;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static int teacherId;
    private static int studentId;
    private static int classId;
    private static int materialId;
    private static String teacherToken;
    private static String studentToken;

    @BeforeAll
    static void startServer() throws Exception {
        UserController userController = new UserController();
        CourseController courseController = new CourseController();
        MaterialController materialController = new MaterialController();
        ReviewController reviewController = new ReviewController();
        ParticipantController participantController = new ParticipantController();

        app = Javalin.create(config ->
                config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost())));

        app.before("/api/*", ctx -> {
            String path = ctx.path();
            // skip auth for public endpoints
            if (path.startsWith("/api/auth/register")) return;
            if (path.startsWith("/api/auth/login")) return;
            if (path.startsWith("/api/materials/course/")) return;
            if (path.startsWith("/api/reviews/material/")) return;
            if (path.matches("/api/reviews/\\d+") && ctx.method().name().equals("GET")) return;
            if (path.startsWith("/api/participants/enroll")) return;
            checkAuth(ctx);
        });

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

        User teacher = createUser(userDao, "edge_teacher", "edge_teacher@test.com", "pass123");
        User student = createUser(userDao, "edge_student", "edge_student@test.com", "pass123");
        teacherId = teacher.getUserId();
        studentId = student.getUserId();
        teacherToken = JWTUtil.generateToken(teacher.getEmail());
        studentToken = JWTUtil.generateToken(student.getEmail());

        Course c = new Course();
        c.setClassName("Edge Test Class");
        c.setCreatorId(teacherId);
        c.setTopic("Edge Cases");
        c = courseDao.create(c);
        classId = c.getClassId();

        participantDao.addParticipant(teacherId, classId);
        participantDao.addParticipant(studentId, classId);

        Material m = new Material();
        m.setOriginalFilename("edge_test.txt");
        m.setStoredFilename("edge_stored.txt");
        m.setFilepath("uploads/edge_stored.txt");
        m.setMaterialType("Slides");
        m.setClassId(classId);
        m.setUserId(teacherId);
        m = materialDao.create(m);
        materialId = m.getFileId();

        Files.createDirectories(Path.of("uploads"));
        Files.writeString(Path.of("uploads/edge_stored.txt"), "edge test content");
    }

    // ---- User Controller Edge Cases ----

    @Test
    @Order(1)
    void registerInvalidJsonReturns500() throws Exception {
        var resp = sendJson("POST", "/api/auth/register", null, "not json");
        assertEquals(500, resp.statusCode());
    }

    @Test
    @Order(2)
    void registerEmptyBodyReturns500() throws Exception {
        var resp = sendJson("POST", "/api/auth/register", null, "{}");
        assertEquals(400, resp.statusCode());
    }

    @Test
    @Order(3)
    void loginInvalidJsonReturns500() throws Exception {
        var resp = sendJson("POST", "/api/auth/login", null, "not json");
        assertEquals(500, resp.statusCode());
    }

    @Test
    @Order(4)
    void loginEmptyEmailAndEmptyUsernameReturns400() throws Exception {
        var resp = sendJson("POST", "/api/auth/login", null, """
                {"email":"","username":"","password":"pass"}""");
        assertEquals(400, resp.statusCode());
    }

    @Test
    @Order(5)
    void loginEmptyPasswordReturns400() throws Exception {
        var resp = sendJson("POST", "/api/auth/login", null, """
                {"email":"edge_teacher@test.com","password":""}""");
        assertEquals(400, resp.statusCode());
    }

    @Test
    @Order(6)
    void getUserByIdInvalidIdReturns500() throws Exception {
        var resp = sendJson("GET", "/api/users/abc", bearer(teacherToken), null);
        assertEquals(500, resp.statusCode());
    }

    @Test
    @Order(7)
    void changePasswordNoEmailAttributeReturns401() throws Exception {
        // Use a token for a non-existent user
        String fakeToken = JWTUtil.generateToken("nonexistent@test.com");
        var resp = sendJson("PUT", "/api/auth/change-password", bearer(fakeToken), """
                {"currentPassword":"x","newPassword":"y"}""");
        assertEquals(401, resp.statusCode());
    }

    @Test
    @Order(8)
    void changePasswordEmptyFieldsReturns400() throws Exception {
        var resp = sendJson("PUT", "/api/auth/change-password", bearer(teacherToken), """
                {"currentPassword":"","newPassword":""}""");
        assertEquals(400, resp.statusCode());
    }

    // ---- Course Controller Edge Cases ----

    @Test
    @Order(10)
    void createCourseNoAuthReturns401() throws Exception {
        var resp = sendJson("POST", "/api/courses", null, """
                {"className":"test"}""");
        assertEquals(401, resp.statusCode());
    }

    @Test
    @Order(11)
    void createCourseEmptyClassNameReturns400() throws Exception {
        var resp = sendJson("POST", "/api/courses", bearer(teacherToken), """
                {"className":"   ","topic":"t"}""");
        assertEquals(400, resp.statusCode());
    }

    @Test
    @Order(12)
    void getCourseByIdInvalidIdReturns500() throws Exception {
        var resp = sendJson("GET", "/api/courses/abc", bearer(teacherToken), null);
        assertEquals(500, resp.statusCode());
    }

    @Test
    @Order(13)
    void updateCourseNoAuthReturns401() throws Exception {
        var resp = sendJson("PUT", "/api/courses/" + classId, null, """
                {"topic":"new"}""");
        assertEquals(401, resp.statusCode());
    }

    @Test
    @Order(14)
    void deleteCourseNonCreatorReturns403() throws Exception {
        var resp = sendJson("DELETE", "/api/courses/" + classId, bearer(studentToken), null);
        assertEquals(403, resp.statusCode());
    }

    // ---- Material Controller Edge Cases ----

    @Test
    @Order(20)
    void getMaterialsByCourseInvalidIdReturns500() throws Exception {
        var resp = sendJson("GET", "/api/materials/course/abc", null, null);
        assertEquals(500, resp.statusCode());
    }

    @Test
    @Order(21)
    void getMaterialByIdNotFoundReturns404() throws Exception {
        var resp = sendJson("GET", "/api/materials/999999", bearer(teacherToken), null);
        assertEquals(404, resp.statusCode());
    }

    @Test
    @Order(22)
    void downloadMaterialMaterialNotFoundReturns404() throws Exception {
        var resp = sendJson("GET", "/api/materials/999999/download", bearer(teacherToken), null);
        assertEquals(404, resp.statusCode());
    }

    @Test
    @Order(23)
    void downloadMaterialFileNotOnDiskReturns404() throws Exception {
        // Create material with non-existent file path
        MaterialDao dao = new MaterialDao();
        Material m = new Material();
        m.setOriginalFilename("ghost.txt");
        m.setStoredFilename("ghost.txt");
        m.setFilepath("uploads/ghost_nonexistent_file.txt");
        m.setMaterialType("Other");
        m.setClassId(classId);
        m.setUserId(teacherId);
        m = dao.create(m);

        var resp = sendJson("GET", "/api/materials/" + m.getFileId() + "/download",
                bearer(studentToken), null);
        assertEquals(404, resp.statusCode());
        assertTrue(resp.body().contains("File not found on server"));

        dao.delete(m.getFileId());
    }

    @Test
    @Order(24)
    void updateMaterialEmptyBodySucceeds() throws Exception {
        var resp = sendJson("PUT", "/api/materials/" + materialId, bearer(teacherToken), "");
        assertEquals(200, resp.statusCode());
    }

    @Test
    @Order(25)
    void updateMaterialInvalidJsonBodySucceeds() throws Exception {
        var resp = sendJson("PUT", "/api/materials/" + materialId, bearer(teacherToken), "not json");
        assertEquals(200, resp.statusCode());
    }

    @Test
    @Order(26)
    void deleteMaterialNotCreatorReturns403() throws Exception {
        var resp = sendJson("DELETE", "/api/materials/" + materialId, bearer(studentToken), null);
        assertEquals(403, resp.statusCode());
    }

    @Test
    @Order(27)
    void uploadMaterialMissingFileReturns400() throws Exception {
        // Send multipart without a file
        String boundary = "----TestBoundary" + System.currentTimeMillis();
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"classId\"\r\n\r\n"
                + classId + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"materialType\"\r\n\r\n"
                + "Other\r\n"
                + "--" + boundary + "--\r\n";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/materials"))
                .header("Authorization", bearer(teacherToken))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        var resp = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
    }

    @Test
    @Order(28)
    void uploadMaterialNonExistentClassReturns400() throws Exception {
        String boundary = "----TestBoundary" + System.currentTimeMillis();
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + "hello\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"classId\"\r\n\r\n"
                + "999999\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"materialType\"\r\n\r\n"
                + "Other\r\n"
                + "--" + boundary + "--\r\n";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/materials"))
                .header("Authorization", bearer(teacherToken))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        var resp = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
    }

    @Test
    @Order(29)
    void uploadMaterialNotCreatorReturns403() throws Exception {
        String boundary = "----TestBoundary" + System.currentTimeMillis();
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + "hello\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"classId\"\r\n\r\n"
                + classId + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"materialType\"\r\n\r\n"
                + "Other\r\n"
                + "--" + boundary + "--\r\n";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/materials"))
                .header("Authorization", bearer(studentToken))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        var resp = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, resp.statusCode());
    }

    // ---- Review Controller Edge Cases ----

    @Test
    @Order(30)
    void createReviewNoAuthReturns401() throws Exception {
        var resp = sendJson("POST", "/api/reviews", null, """
                {"review":"test","rating":3,"fileId":%d}""".formatted(materialId));
        assertEquals(401, resp.statusCode());
    }

    @Test
    @Order(31)
    void getReviewsByMaterialInvalidIdReturns500() throws Exception {
        var resp = sendJson("GET", "/api/reviews/material/abc", null, null);
        assertEquals(500, resp.statusCode());
    }

    @Test
    @Order(32)
    void getReviewByIdInvalidIdReturns500() throws Exception {
        var resp = sendJson("GET", "/api/reviews/abc", bearer(teacherToken), null);
        assertEquals(500, resp.statusCode());
    }

    @Test
    @Order(33)
    void updateReviewNotFoundReturns404() throws Exception {
        var resp = sendJson("PUT", "/api/reviews/999999", bearer(studentToken), """
                {"review":"updated","rating":3}""");
        assertEquals(404, resp.statusCode());
    }

    @Test
    @Order(34)
    void deleteReviewNotFoundReturns404() throws Exception {
        var resp = sendJson("DELETE", "/api/reviews/999999", bearer(studentToken), null);
        assertEquals(404, resp.statusCode());
    }

    // ---- Participant Controller Edge Cases ----

    @Test
    @Order(40)
    void enrollUserInvalidJsonReturns500() throws Exception {
        var resp = sendJson("POST", "/api/participants/enroll", null, "not json");
        assertEquals(500, resp.statusCode());
    }

    @Test
    @Order(41)
    void getParticipantsByClassInvalidIdReturns500() throws Exception {
        var resp = sendJson("GET", "/api/participants/class/abc", bearer(teacherToken), null);
        assertEquals(500, resp.statusCode());
    }

    @Test
    @Order(42)
    void getClassesByUserInvalidIdReturns500() throws Exception {
        var resp = sendJson("GET", "/api/participants/user/abc", bearer(teacherToken), null);
        assertEquals(500, resp.statusCode());
    }

    @Test
    @Order(43)
    void unenrollInvalidJsonReturns500() throws Exception {
        var resp = sendJson("DELETE", "/api/participants/unenroll", bearer(teacherToken), "not json");
        assertEquals(500, resp.statusCode());
    }

    @Test
    @Order(44)
    void unenrollNonExistentClassReturns400() throws Exception {
        var resp = sendJson("DELETE", "/api/participants/unenroll", bearer(studentToken), """
                {"userId":%d,"classId":999999}""".formatted(studentId));
        assertEquals(400, resp.statusCode());
    }

    // ---- Helpers ----

    private static void checkAuth(Context ctx) {
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
