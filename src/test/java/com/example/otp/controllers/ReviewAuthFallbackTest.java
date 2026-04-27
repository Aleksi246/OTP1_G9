package com.example.otp.controllers;

import com.example.otp.dao.*;
import com.example.otp.db.Database;
import com.example.otp.model.*;
import com.example.otp.util.BCryptUtil;
import com.example.otp.util.JWTUtil;
import io.javalin.Javalin;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests ReviewController.getAuthenticatedUser fallback logic - when email attribute
 * is NOT set by middleware, the controller falls back to parsing the Authorization header.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReviewAuthFallbackTest {

    private static Javalin app;
    private static String baseUrl;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static int materialId;
    private static int reviewId;
    private static int studentId;
    private static String studentToken;

    @BeforeAll
    static void startServer() throws Exception {
        ReviewController reviewController = new ReviewController();
        MaterialDao materialDao = new MaterialDao();
        CourseDao courseDao = new CourseDao();
        UserDao userDao = new UserDao();
        ReviewDao reviewDao = new ReviewDao();
        ParticipantDao participantDao = new ParticipantDao();

        app = Javalin.create(config ->
                config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost())));

        // NO auth middleware - so email attribute will NOT be set
        // This forces ReviewController.getAuthenticatedUser to use its internal fallback
        app.get("/api/reviews/material/{fileId}", reviewController::getReviewsByMaterial);
        app.get("/api/reviews/{id}", reviewController::getReviewById);
        app.post("/api/reviews", reviewController::createReview);
        app.put("/api/reviews/{id}", reviewController::updateReview);
        app.delete("/api/reviews/{id}", reviewController::deleteReview);

        app.start(0);
        baseUrl = "http://localhost:" + app.port();

        // Seed data
        clearAllTables();
        User student = new User();
        student.setUsername("review_auth_student");
        student.setEmail("review_auth_student@test.com");
        student.setPasswordHash(BCryptUtil.hashPassword("pass123"));
        student = userDao.create(student);
        studentId = student.getUserId();
        studentToken = JWTUtil.generateToken(student.getEmail());

        Course c = new Course();
        c.setClassName("Review Auth Test");
        c.setCreatorId(studentId);
        c.setTopic("Auth Fallback");
        c = courseDao.create(c);
        participantDao.addParticipant(studentId, c.getClassId());

        Material m = new Material();
        m.setOriginalFilename("auth_test.txt");
        m.setStoredFilename("auth_stored.txt");
        m.setFilepath("uploads/auth_stored.txt");
        m.setMaterialType("Slides");
        m.setClassId(c.getClassId());
        m.setUserId(studentId);
        m = materialDao.create(m);
        materialId = m.getFileId();
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (app != null) app.stop();
        clearAllTables();
    }

    // Test: getAuthenticatedUser falls back to Authorization header parsing
    @Test
    @Order(1)
    void createReviewViaBearerHeaderFallbackReturns201() throws Exception {
        var resp = sendJson("POST", "/api/reviews", "Bearer " + studentToken, """
                {"review":"Fallback auth test","rating":4,"fileId":%d}""".formatted(materialId));
        assertEquals(201, resp.statusCode());
        assertTrue(resp.body().contains("reviewId"));
        // Save reviewId for later tests
        var json = com.google.gson.JsonParser.parseString(resp.body()).getAsJsonObject();
        reviewId = json.get("reviewId").getAsInt();
    }

    // Test: getAuthenticatedUser with no Authorization header returns null → 401
    @Test
    @Order(2)
    void createReviewNoAuthHeaderReturns401() throws Exception {
        var resp = sendJson("POST", "/api/reviews", null, """
                {"review":"No auth","rating":3,"fileId":%d}""".formatted(materialId));
        assertEquals(401, resp.statusCode());
    }

    // Test: getAuthenticatedUser with invalid Bearer token
    @Test
    @Order(3)
    void createReviewInvalidBearerTokenReturns401() throws Exception {
        var resp = sendJson("POST", "/api/reviews", "Bearer invalid.token.here", """
                {"review":"Invalid token","rating":3,"fileId":%d}""".formatted(materialId));
        assertEquals(401, resp.statusCode());
    }

    // Test: getAuthenticatedUser with malformed Authorization header (no "Bearer " prefix)
    @Test
    @Order(4)
    void createReviewMalformedAuthHeaderReturns401() throws Exception {
        var resp = sendJson("POST", "/api/reviews", "Basic dXNlcjpwYXNz", """
                {"review":"Basic auth","rating":3,"fileId":%d}""".formatted(materialId));
        assertEquals(401, resp.statusCode());
    }

    // Test: getAuthenticatedUser with token for non-existent user
    @Test
    @Order(5)
    void createReviewTokenForNonExistentUserReturns401() throws Exception {
        String badToken = JWTUtil.generateToken("nonexistent@test.com");
        var resp = sendJson("POST", "/api/reviews", "Bearer " + badToken, """
                {"review":"Ghost user","rating":3,"fileId":%d}""".formatted(materialId));
        assertEquals(401, resp.statusCode());
    }

    // Test: updateReview via fallback auth
    @Test
    @Order(6)
    void updateReviewViaBearerFallbackReturns200() throws Exception {
        var resp = sendJson("PUT", "/api/reviews/" + reviewId, "Bearer " + studentToken, """
                {"review":"Updated via fallback","rating":5}""");
        assertEquals(200, resp.statusCode());
    }

    // Test: deleteReview via fallback auth
    @Test
    @Order(7)
    void deleteReviewViaBearerFallbackReturns200() throws Exception {
        var resp = sendJson("DELETE", "/api/reviews/" + reviewId, "Bearer " + studentToken, null);
        assertEquals(200, resp.statusCode());
    }

    // ---- Helpers ----

    private static void clearAllTables() throws Exception {
        try (Connection c = Database.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM reviews");
            s.executeUpdate("DELETE FROM materials");
            s.executeUpdate("DELETE FROM participants");
            s.executeUpdate("DELETE FROM classes");
            s.executeUpdate("DELETE FROM users");
        }
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
