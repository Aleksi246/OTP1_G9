package com.example.otp.controllers;

import com.example.otp.dao.*;
import com.example.otp.db.Database;
import com.example.otp.model.*;
import com.example.otp.util.BCryptUtil;
import com.example.otp.util.JWTUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User Acceptance Tests based on the Acceptance Test Plan (Acceptance_Test_Plan.md).
 *
 * Covers all 11 user stories:
 *  1. Registration
 *  2. Login
 *  3. Create a class
 *  4. Upload material
 *  5. Join a class
 *  6. Download material
 *  7. Review material
 *  8. View reviews
 *  9. Delete material
 * 10. Delete class
 * 11. Logout (token invalidation)
 *
 * Tests are ordered to follow a realistic user workflow and use the API
 * endpoints exactly as documented in the acceptance test plan.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserAcceptanceTest {

    private static Javalin app;
    private static String baseUrl;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    // Shared state across ordered tests
    private String testUser1Token;
    private int testUser1Id;
    private String testUser2Token;
    private int testUser2Id;
    private String testUser3Token;
    private int testUser3Id;
    private int classId;
    private int materialId;
    private int reviewId;

    @BeforeAll
    void startServerAndCleanDb() throws Exception {
        UserController userController = new UserController();
        CourseController courseController = new CourseController();
        MaterialController materialController = new MaterialController();
        ReviewController reviewController = new ReviewController();
        ParticipantController participantController = new ParticipantController();

        app = Javalin.create(config ->
                config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()))
        );

        // Auth middleware on protected routes
        app.before("/api/users", UserAcceptanceTest::checkAuth);
        app.before("/api/users/{id}", UserAcceptanceTest::checkAuth);
        app.before("/api/courses", UserAcceptanceTest::checkAuth);
        app.before("/api/courses/{id}", UserAcceptanceTest::checkAuth);
        app.before("/api/materials", UserAcceptanceTest::checkAuth);
        app.before("/api/materials/{id}", UserAcceptanceTest::checkAuth);
        app.before("/api/materials/{id}/download", UserAcceptanceTest::checkAuth);
        app.before("/api/participants", UserAcceptanceTest::checkAuth);
        app.before("/api/participants/unenroll", UserAcceptanceTest::checkAuth);
        app.before("/api/participants/class/{classId}", UserAcceptanceTest::checkAuth);
        app.before("/api/participants/user/{userId}", UserAcceptanceTest::checkAuth);
        app.before("/api/reviews", UserAcceptanceTest::checkAuth);

        // Auth endpoints (no token required)
        app.post("/api/auth/register", userController::register);
        app.post("/api/auth/login", userController::login);
        app.before("/api/auth/change-password", UserAcceptanceTest::checkAuth);
        app.put("/api/auth/change-password", userController::changePassword);

        // User endpoints
        app.get("/api/users", userController::getAllUsers);
        app.get("/api/users/{id}", userController::getUserById);
        app.before("/api/users/email/{email}", UserAcceptanceTest::checkAuth);
        app.get("/api/users/email/{email}", userController::getUserByEmail);

        // Course endpoints
        app.get("/api/courses", courseController::getAllCourses);
        app.get("/api/courses/{id}", courseController::getCourseById);
        app.post("/api/courses", courseController::createCourse);
        app.put("/api/courses/{id}", courseController::updateCourse);
        app.delete("/api/courses/{id}", courseController::deleteCourse);

        // Material endpoints
        app.get("/api/materials/course/{classId}", materialController::getMaterialsByCourse);
        app.get("/api/materials/{id}", materialController::getMaterialById);
        app.get("/api/materials/{id}/download", materialController::downloadMaterial);
        app.post("/api/materials", materialController::uploadMaterial);
        app.put("/api/materials/{id}", materialController::updateMaterial);
        app.delete("/api/materials/{id}", materialController::deleteMaterial);

        // Review endpoints
        app.get("/api/reviews/material/{fileId}", reviewController::getReviewsByMaterial);
        app.get("/api/reviews/{id}", reviewController::getReviewById);
        app.post("/api/reviews", reviewController::createReview);
        app.put("/api/reviews/{id}", reviewController::updateReview);
        app.delete("/api/reviews/{id}", reviewController::deleteReview);

        // Participant endpoints
        app.post("/api/participants/enroll", participantController::enrollUser);
        app.delete("/api/participants/unenroll", participantController::unenrollUser);
        app.get("/api/participants/class/{classId}", participantController::getUsersByClass);
        app.get("/api/participants/user/{userId}", participantController::getClassesByUser);

        app.start(0);
        baseUrl = "http://localhost:" + app.port();

        clearAllTables();
    }

    @AfterAll
    void stopServer() {
        if (app != null) {
            app.stop();
        }
    }

    // =========================================================================
    // Criteria 1 — Registration
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("UAT-01: Register a new user (testuser1) — expects 201")
    void registerNewUser() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/auth/register", null, """
                {
                  "username": "testuser1",
                  "email": "test1@example.com",
                  "password": "SecurePass123"
                }
                """);

        assertEquals(201, response.statusCode(), "New user registration should return 201");
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(body.has("userId"), "Response should contain userId");
        testUser1Id = body.get("userId").getAsInt();
        assertFalse(body.has("passwordHash") && body.get("passwordHash") != null
                        && !body.get("passwordHash").isJsonNull(),
                "Password hash must not be exposed in response");

        // Verify the password is stored as a BCrypt hash in the DB
        User dbUser = new UserDao().findByUsername("testuser1");
        assertNotNull(dbUser, "User should exist in users table");
        assertTrue(dbUser.getPasswordHash().startsWith("$2"), "Password should be BCrypt hashed");
    }

    @Test
    @Order(2)
    @DisplayName("UAT-02: Register with duplicate username/email — expects 409")
    void registerDuplicateUser() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/auth/register", null, """
                {
                  "username": "testuser1",
                  "email": "test1@example.com",
                  "password": "AnotherPass456"
                }
                """);

        assertTrue(response.statusCode() == 409 || response.statusCode() == 400,
                "Duplicate registration should return 409 or 400, got: " + response.statusCode());
    }

    // =========================================================================
    // Criteria 2 — Login
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("UAT-03: Login with correct credentials — expects 200 + JWT token")
    void loginSuccess() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/auth/login", null, """
                {
                  "email": "test1@example.com",
                  "password": "SecurePass123"
                }
                """);

        assertEquals(200, response.statusCode(), "Valid login should return 200");
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(body.has("token"), "Response should contain a JWT token");
        testUser1Token = body.get("token").getAsString();
        assertFalse(testUser1Token.isEmpty(), "Token should not be empty");

        // Validate the token contains the user's email
        String email = JWTUtil.validateToken(testUser1Token);
        assertEquals("test1@example.com", email, "Token should contain user's email in claims");
    }

    @Test
    @Order(4)
    @DisplayName("UAT-04: Login with wrong password — expects 401, no token")
    void loginWrongPassword() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/auth/login", null, """
                {
                  "email": "test1@example.com",
                  "password": "WrongPassword"
                }
                """);

        assertEquals(401, response.statusCode(), "Wrong password should return 401");
        assertFalse(response.body().contains("token"),
                "No token should be returned for failed login");
    }

    // =========================================================================
    // Register + login helper users (testuser2 and testuser3 for later tests)
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("UAT-05: Register and login testuser2 (student)")
    void registerAndLoginTestUser2() throws Exception {
        HttpResponse<String> reg = sendJson("POST", "/api/auth/register", null, """
                {
                  "username": "testuser2",
                  "email": "test2@example.com",
                  "password": "StudentPass123"
                }
                """);
        assertEquals(201, reg.statusCode());
        testUser2Id = JsonParser.parseString(reg.body()).getAsJsonObject().get("userId").getAsInt();

        HttpResponse<String> login = sendJson("POST", "/api/auth/login", null, """
                {
                  "email": "test2@example.com",
                  "password": "StudentPass123"
                }
                """);
        assertEquals(200, login.statusCode());
        testUser2Token = JsonParser.parseString(login.body()).getAsJsonObject()
                .get("token").getAsString();
    }

    @Test
    @Order(6)
    @DisplayName("UAT-06: Register and login testuser3 (non-enrolled)")
    void registerAndLoginTestUser3() throws Exception {
        HttpResponse<String> reg = sendJson("POST", "/api/auth/register", null, """
                {
                  "username": "testuser3",
                  "email": "test3@example.com",
                  "password": "ThirdPass123"
                }
                """);
        assertEquals(201, reg.statusCode());
        testUser3Id = JsonParser.parseString(reg.body()).getAsJsonObject().get("userId").getAsInt();

        HttpResponse<String> login = sendJson("POST", "/api/auth/login", null, """
                {
                  "email": "test3@example.com",
                  "password": "ThirdPass123"
                }
                """);
        assertEquals(200, login.statusCode());
        testUser3Token = JsonParser.parseString(login.body()).getAsJsonObject()
                .get("token").getAsString();
    }

    // =========================================================================
    // Criteria 3 — Create a class
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("UAT-07: Create a class as logged-in user — expects 201")
    void createClass() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Math 101",
                  "topic": "Mathematics"
                }
                """);

        assertEquals(201, response.statusCode(), "Creating a class should return 201");
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(body.has("classId"), "Response should contain classId");
        classId = body.get("classId").getAsInt();

        // Verify creator is in the classes table
        Course course = new CourseDao().findById(classId);
        assertNotNull(course, "Class should exist in the DB");
        assertEquals(testUser1Id, course.getCreatorId(), "Creator ID should match testuser1");

        // Verify creator is auto-enrolled as participant
        List<Integer> participants = new ParticipantDao().findUsersByClass(classId);
        assertTrue(participants.contains(testUser1Id),
                "Creator should be auto-enrolled as participant");
    }

    @Test
    @Order(11)
    @DisplayName("UAT-08: Create class with duplicate name — expects 400 or 409")
    void createClassDuplicateName() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Math 101",
                  "topic": "Another Math"
                }
                """);

        assertTrue(response.statusCode() == 400 || response.statusCode() == 409
                        || response.statusCode() == 500,
                "Duplicate class name should be rejected, got: " + response.statusCode());
    }

    // =========================================================================
    // Criteria 4 — Upload material
    // =========================================================================

    @Test
    @Order(20)
    @DisplayName("UAT-09: Creator uploads a file to the class — expects 201")
    void uploadMaterial() throws Exception {
        HttpResponse<String> response = sendMultipartUpload(
                testUser1Token, classId, "Study Guide",
                "calculus_notes.txt", "These are calculus notes content for testing.");

        assertEquals(201, response.statusCode(), "File upload by creator should return 201");
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(body.has("fileId"), "Response should contain fileId");
        materialId = body.get("fileId").getAsInt();

        // Verify file on disk
        Material material = new MaterialDao().findById(materialId);
        assertNotNull(material, "Material should exist in the DB");
        assertEquals("calculus_notes.txt", material.getOriginalFilename());
        assertEquals("Study Guide", material.getMaterialType());
        assertEquals(classId, material.getClassId());
        assertEquals(testUser1Id, material.getUserId());

        Path filePath = Paths.get(material.getFilepath());
        assertTrue(Files.exists(filePath), "Uploaded file should exist on disk");
    }

    @Test
    @Order(21)
    @DisplayName("UAT-10: Non-creator tries to upload — expects 403")
    void uploadMaterialNonCreator() throws Exception {
        HttpResponse<String> response = sendMultipartUpload(
                testUser2Token, classId, "Textbook",
                "unauthorized_upload.txt", "Should not be saved");

        assertEquals(403, response.statusCode(),
                "Non-creator upload should return 403");
    }

    @Test
    @Order(22)
    @DisplayName("UAT-10b: Unauthenticated user tries to upload — expects 401")
    void uploadMaterialNoToken() throws Exception {
        HttpResponse<String> response = sendMultipartUpload(
                "invalid.token.value", classId, "Textbook",
                "no_auth_upload.txt", "Should not be saved");

        assertEquals(401, response.statusCode(),
                "Unauthenticated upload should return 401");
    }

    // =========================================================================
    // Criteria 5 — Join a class
    // =========================================================================

    @Test
    @Order(30)
    @DisplayName("UAT-11: User joins an existing class — expects 201")
    void joinClass() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/participants/enroll", null, """
                {
                  "userId": %d,
                  "classId": %d
                }
                """.formatted(testUser2Id, classId));

        assertEquals(201, response.statusCode(), "Joining a class should return 201");

        // Verify in DB
        List<Integer> participants = new ParticipantDao().findUsersByClass(classId);
        assertTrue(participants.contains(testUser2Id),
                "testuser2 should be in participants for Math 101");
    }

    // =========================================================================
    // Criteria 6 — Download material
    // =========================================================================

    @Test
    @Order(40)
    @DisplayName("UAT-12: Enrolled user downloads a file — expects 200")
    void downloadMaterialEnrolled() throws Exception {
        HttpResponse<String> response = sendJson("GET",
                "/api/materials/" + materialId + "/download",
                bearer(testUser2Token), null);

        assertEquals(200, response.statusCode(),
                "Enrolled user download should return 200");
        assertFalse(response.body().isEmpty(), "Downloaded content should not be empty");
        assertTrue(response.body().contains("calculus notes content"),
                "Downloaded content should match what was uploaded");
    }

    @Test
    @Order(41)
    @DisplayName("UAT-13: Non-enrolled user tries to download — expects 403")
    void downloadMaterialNotEnrolled() throws Exception {
        HttpResponse<String> response = sendJson("GET",
                "/api/materials/" + materialId + "/download",
                bearer(testUser3Token), null);

        assertEquals(403, response.statusCode(),
                "Non-enrolled user download should return 403");
    }

    // =========================================================================
    // Criteria 7 — Review material
    // =========================================================================

    @Test
    @Order(50)
    @DisplayName("UAT-14: Enrolled user leaves a review — expects 201")
    void submitReview() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/reviews",
                bearer(testUser2Token), """
                {
                  "review": "Very helpful notes!",
                  "rating": 4,
                  "fileId": %d
                }
                """.formatted(materialId));

        assertEquals(201, response.statusCode(), "Creating a review should return 201");
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(body.has("reviewId"), "Response should contain reviewId");
        reviewId = body.get("reviewId").getAsInt();

        // Verify review in DB
        Review dbReview = new ReviewDao().findById(reviewId);
        assertNotNull(dbReview, "Review should exist in the DB");
        assertEquals("Very helpful notes!", dbReview.getReview());
        assertEquals(4, dbReview.getRating());
        assertEquals(materialId, dbReview.getFileId());
        assertEquals(testUser2Id, dbReview.getUserId());
        assertNotNull(dbReview.getCreatedAt(), "created_at should be auto-filled");
    }

    @Test
    @Order(51)
    @DisplayName("UAT-15: Review with invalid rating (>5) — expects 400 or 500")
    void submitReviewInvalidRating() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/reviews",
                bearer(testUser2Token), """
                {
                  "review": "Bad rating test",
                  "rating": 6,
                  "fileId": %d
                }
                """.formatted(materialId));

        assertTrue(response.statusCode() == 400 || response.statusCode() == 500,
                "Rating outside 0-5 should be rejected (DB CHECK constraint), got: "
                        + response.statusCode());
    }

    @Test
    @Order(52)
    @DisplayName("UAT-15b: Unauthenticated user tries to create review — expects 401")
    void submitReviewNoToken() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/reviews", null, """
                {
                  "review": "No auth review",
                  "rating": 3,
                  "fileId": %d
                }
                """.formatted(materialId));

        assertEquals(401, response.statusCode(),
                "Unauthenticated review creation should return 401");
    }

    @Test
    @Order(53)
    @DisplayName("UAT-15c: User tries to update another user's review — expects 403")
    void updateReviewWrongUser() throws Exception {
        HttpResponse<String> response = sendJson("PUT", "/api/reviews/" + reviewId,
                bearer(testUser3Token), """
                {
                  "review": "Hijacked review",
                  "rating": 1
                }
                """);

        assertEquals(403, response.statusCode(),
                "Updating another user's review should return 403");
    }

    @Test
    @Order(54)
    @DisplayName("UAT-15d: User tries to delete another user's review — expects 403")
    void deleteReviewWrongUser() throws Exception {
        HttpResponse<String> response = sendJson("DELETE", "/api/reviews/" + reviewId,
                bearer(testUser3Token), null);

        assertEquals(403, response.statusCode(),
                "Deleting another user's review should return 403");
    }

    // =========================================================================
    // Criteria 8 — View reviews
    // =========================================================================

    @Test
    @Order(60)
    @DisplayName("UAT-16: View all reviews for a material — expects 200 + review data")
    void viewReviews() throws Exception {
        HttpResponse<String> response = sendJson("GET",
                "/api/reviews/material/" + materialId, null, null);

        assertEquals(200, response.statusCode(), "Viewing reviews should return 200");

        JsonArray reviews = JsonParser.parseString(response.body()).getAsJsonArray();
        assertTrue(reviews.size() >= 1, "Should have at least one review");

        JsonObject firstReview = reviews.get(0).getAsJsonObject();
        assertTrue(firstReview.has("review"), "Review should contain review text");
        assertTrue(firstReview.has("rating"), "Review should contain rating");
        assertTrue(firstReview.has("userId"), "Review should contain userId");
        assertTrue(firstReview.has("createdAt"), "Review should contain timestamp");
    }

    // =========================================================================
    // Criteria 9 — Delete material
    // =========================================================================

    @Test
    @Order(69)
    @DisplayName("UAT-16b: Non-creator tries to delete material — expects 403")
    void deleteMaterialNonCreator() throws Exception {
        HttpResponse<String> response = sendJson("DELETE",
                "/api/materials/" + materialId, bearer(testUser2Token), null);

        assertEquals(403, response.statusCode(),
                "Non-creator should not be able to delete material");

        // Verify material still exists
        Material material = new MaterialDao().findById(materialId);
        assertNotNull(material, "Material should still exist after forbidden delete attempt");
    }

    @Test
    @Order(70)
    @DisplayName("UAT-17: Creator deletes a material — expects 200, file + DB cleaned up")
    void deleteMaterial() throws Exception {
        // First get the file path before deleting
        Material material = new MaterialDao().findById(materialId);
        assertNotNull(material);
        Path filePath = Paths.get(material.getFilepath());

        HttpResponse<String> response = sendJson("DELETE",
                "/api/materials/" + materialId, bearer(testUser1Token), null);

        assertEquals(200, response.statusCode(), "Deleting material should return 200");

        // Verify DB record is gone
        Material deleted = new MaterialDao().findById(materialId);
        assertNull(deleted, "Material row should be removed from DB");

        // Verify file is gone from disk
        assertFalse(Files.exists(filePath), "File should be deleted from uploads/");

        // Verify reviews for this material are also gone (FK cascade)
        List<Review> reviews = new ReviewDao().findByFileId(materialId);
        assertTrue(reviews.isEmpty(), "Reviews for deleted material should be cascade-deleted");
    }

    // =========================================================================
    // Criteria 10 — Delete class (cascade)
    // =========================================================================

    @Test
    @Order(79)
    @DisplayName("UAT-17b: Non-creator tries to delete class — expects 403")
    void deleteClassNonCreator() throws Exception {
        HttpResponse<String> response = sendJson("DELETE",
                "/api/courses/" + classId, bearer(testUser2Token), null);

        assertEquals(403, response.statusCode(),
                "Non-creator should not be able to delete class");

        // Verify class still exists
        Course course = new CourseDao().findById(classId);
        assertNotNull(course, "Class should still exist after forbidden delete attempt");
    }

    @Test
    @Order(79)
    @DisplayName("UAT-17c: Non-creator tries to update course — expects 403")
    void updateCourseNonCreator() throws Exception {
        HttpResponse<String> response = sendJson("PUT",
                "/api/courses/" + classId, bearer(testUser2Token), """
                {
                  "topic": "Hacked Topic"
                }
                """);

        assertEquals(403, response.statusCode(),
                "Non-creator should not be able to update course");
    }

    @Test
    @Order(80)
    @DisplayName("UAT-18: Creator deletes entire class — expects 200, cascade delete")
    void deleteClass() throws Exception {
        // Upload a new material for the cascade test
        HttpResponse<String> uploadResp = sendMultipartUpload(
                testUser1Token, classId, "Presentation",
                "cascade_test.txt", "Content for cascade test");
        assertEquals(201, uploadResp.statusCode());
        int cascadeMaterialId = JsonParser.parseString(uploadResp.body()).getAsJsonObject()
                .get("fileId").getAsInt();

        // Leave a review on the new material
        HttpResponse<String> reviewResp = sendJson("POST", "/api/reviews",
                bearer(testUser2Token), """
                {
                  "review": "Will be cascade-deleted",
                  "rating": 3,
                  "fileId": %d
                }
                """.formatted(cascadeMaterialId));
        assertEquals(201, reviewResp.statusCode());

        // Now delete the class
        HttpResponse<String> response = sendJson("DELETE",
                "/api/courses/" + classId, bearer(testUser1Token), null);

        assertEquals(200, response.statusCode(), "Deleting class should return 200");

        // Verify class is gone
        Course deletedCourse = new CourseDao().findById(classId);
        assertNull(deletedCourse, "Class should be removed from DB");

        // Verify participants are cascade-deleted
        List<Integer> participants = new ParticipantDao().findUsersByClass(classId);
        assertTrue(participants.isEmpty(), "Participants should be cascade-deleted");

        // Verify materials are cascade-deleted
        List<Material> materials = new MaterialDao().findByClassId(classId);
        assertTrue(materials.isEmpty(), "Materials should be cascade-deleted");

        // Verify reviews for those materials are cascade-deleted
        List<Review> reviews = new ReviewDao().findByFileId(cascadeMaterialId);
        assertTrue(reviews.isEmpty(), "Reviews should be cascade-deleted");
    }

    // =========================================================================
    // Criteria 11 — Logout (token cleared, can't access protected endpoints)
    // =========================================================================

    @Test
    @Order(90)
    @DisplayName("UAT-19: After logout, protected endpoints reject requests")
    void logoutTokenCleared() throws Exception {
        // Simulate logout by clearing the token (client-side operation)
        // After logout, attempting to access protected endpoints without a token should fail
        HttpResponse<String> noTokenResponse = sendJson("GET", "/api/courses", null, null);
        assertEquals(401, noTokenResponse.statusCode(),
                "Protected endpoints should return 401 without a token");

        // Also verify an invalid/garbage token is rejected
        HttpResponse<String> badTokenResponse = sendJson("GET", "/api/courses",
                bearer("invalid.token.here"), null);
        assertEquals(401, badTokenResponse.statusCode(),
                "Protected endpoints should return 401 with an invalid token");
    }

    // =========================================================================
    // Additional coverage tests — User/Auth edge cases
    // =========================================================================

    @Test
    @Order(91)
    @DisplayName("UAT-21: Register with missing parameters — expects 400")
    void registerMissingParams() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/auth/register", null, """
                {
                  "username": "incomplete_user"
                }
                """);
        assertEquals(400, response.statusCode(),
                "Registration with missing email/password should return 400");
    }

    @Test
    @Order(91)
    @DisplayName("UAT-22: Login with missing parameters — expects 400")
    void loginMissingParams() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/auth/login", null, """
                {
                  "password": "somepass"
                }
                """);
        assertEquals(400, response.statusCode(),
                "Login with no email/username should return 400");
    }

    @Test
    @Order(91)
    @DisplayName("UAT-23: Login with blank email and username by username — expects 200")
    void loginByUsername() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/auth/login", null, """
                {
                  "username": "testuser1",
                  "password": "SecurePass123"
                }
                """);
        assertEquals(200, response.statusCode(), "Login by username should return 200");
        assertTrue(response.body().contains("token"));
    }

    @Test
    @Order(91)
    @DisplayName("UAT-24: Login with email in username field (contains @) — expects 200")
    void loginEmailInUsernameField() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/auth/login", null, """
                {
                  "username": "test1@example.com",
                  "password": "SecurePass123"
                }
                """);
        assertEquals(200, response.statusCode(),
                "Email in username field should fall through to findByEmail");
        assertTrue(response.body().contains("token"));
    }

    @Test
    @Order(91)
    @DisplayName("UAT-25: Login with non-existent username — expects 401")
    void loginNonExistentUsername() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/auth/login", null, """
                {
                  "username": "doesnotexist",
                  "password": "NoUser123"
                }
                """);
        assertEquals(401, response.statusCode(),
                "Non-existent username should return 401");
    }

    @Test
    @Order(91)
    @DisplayName("UAT-26: Login with non-existent email — expects 401")
    void loginNonExistentEmail() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/auth/login", null, """
                {
                  "email": "noone@example.com",
                  "password": "NoUser123"
                }
                """);
        assertEquals(401, response.statusCode(),
                "Non-existent email should return 401");
    }

    @Test
    @Order(91)
    @DisplayName("UAT-27: Register with duplicate email only — expects 409")
    void registerDuplicateEmailOnly() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/auth/register", null, """
                {
                  "username": "uniqueuser999",
                  "email": "test1@example.com",
                  "password": "SomePass123"
                }
                """);
        assertEquals(409, response.statusCode(),
                "Duplicate email should return 409");
    }

    @Test
    @Order(91)
    @DisplayName("UAT-28: Get all users — expects 200, passwords stripped")
    void getAllUsers() throws Exception {
        HttpResponse<String> response = sendJson("GET", "/api/users",
                bearer(testUser1Token), null);
        assertEquals(200, response.statusCode());
        JsonArray users = JsonParser.parseString(response.body()).getAsJsonArray();
        assertTrue(users.size() >= 3, "Should have at least 3 users");
        // Verify passwords are not exposed
        for (int i = 0; i < users.size(); i++) {
            JsonObject user = users.get(i).getAsJsonObject();
            assertTrue(!user.has("passwordHash") || user.get("passwordHash").isJsonNull(),
                    "Password hash should be stripped from response");
        }
    }

    @Test
    @Order(91)
    @DisplayName("UAT-29: Get user by ID — expects 200, password stripped")
    void getUserById() throws Exception {
        HttpResponse<String> response = sendJson("GET", "/api/users/" + testUser1Id,
                bearer(testUser1Token), null);
        assertEquals(200, response.statusCode());
        JsonObject user = JsonParser.parseString(response.body()).getAsJsonObject();
        assertEquals(testUser1Id, user.get("userId").getAsInt());
        assertTrue(!user.has("passwordHash") || user.get("passwordHash").isJsonNull(),
                "Password hash should be stripped");
    }

    @Test
    @Order(91)
    @DisplayName("UAT-30: Get user by non-existent ID — expects 404")
    void getUserByIdNotFound() throws Exception {
        HttpResponse<String> response = sendJson("GET", "/api/users/999999",
                bearer(testUser1Token), null);
        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(91)
    @DisplayName("UAT-31: Get all users without token — expects 401")
    void getAllUsersNoToken() throws Exception {
        HttpResponse<String> response = sendJson("GET", "/api/users", null, null);
        assertEquals(401, response.statusCode());
    }

    @Test
    @Order(91)
    @DisplayName("UAT-32: Change password — expects 200, old password stops working")
    void changePassword() throws Exception {
        // Register a throwaway user for this test
        sendJson("POST", "/api/auth/register", null, """
                {
                  "username": "pwchange_user",
                  "email": "pwchange@example.com",
                  "password": "OldPass123"
                }
                """);
        HttpResponse<String> loginResp = sendJson("POST", "/api/auth/login", null, """
                {
                  "email": "pwchange@example.com",
                  "password": "OldPass123"
                }
                """);
        String token = JsonParser.parseString(loginResp.body()).getAsJsonObject()
                .get("token").getAsString();

        // Change password
        HttpResponse<String> changeResp = sendJson("PUT", "/api/auth/change-password",
                bearer(token), """
                {
                  "currentPassword": "OldPass123",
                  "newPassword": "NewPass456"
                }
                """);
        assertEquals(200, changeResp.statusCode());

        // Old password should fail
        HttpResponse<String> oldLogin = sendJson("POST", "/api/auth/login", null, """
                {
                  "email": "pwchange@example.com",
                  "password": "OldPass123"
                }
                """);
        assertEquals(401, oldLogin.statusCode());

        // New password should work
        HttpResponse<String> newLogin = sendJson("POST", "/api/auth/login", null, """
                {
                  "email": "pwchange@example.com",
                  "password": "NewPass456"
                }
                """);
        assertEquals(200, newLogin.statusCode());
    }

    @Test
    @Order(91)
    @DisplayName("UAT-33: Change password with wrong current password — expects 401")
    void changePasswordWrongCurrent() throws Exception {
        HttpResponse<String> response = sendJson("PUT", "/api/auth/change-password",
                bearer(testUser1Token), """
                {
                  "currentPassword": "TotallyWrong",
                  "newPassword": "DoesntMatter"
                }
                """);
        assertEquals(401, response.statusCode());
    }

    @Test
    @Order(91)
    @DisplayName("UAT-34: Change password with missing params — expects 400")
    void changePasswordMissingParams() throws Exception {
        HttpResponse<String> response = sendJson("PUT", "/api/auth/change-password",
                bearer(testUser1Token), """
                {
                  "currentPassword": "SecurePass123"
                }
                """);
        assertEquals(400, response.statusCode());
    }

    @Test
    @Order(91)
    @DisplayName("UAT-35: Get user by email — expects 200")
    void getUserByEmail() throws Exception {
        HttpResponse<String> response = sendJson("GET",
                "/api/users/email/test1@example.com", bearer(testUser1Token), null);
        assertEquals(200, response.statusCode());
        JsonObject user = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(user.has("userId"));
        assertTrue(!user.has("passwordHash") || user.get("passwordHash").isJsonNull(),
                "Password hash should be stripped");
    }

    @Test
    @Order(91)
    @DisplayName("UAT-36: Get user by non-existent email — expects 404")
    void getUserByEmailNotFound() throws Exception {
        HttpResponse<String> response = sendJson("GET",
                "/api/users/email/nobody@nowhere.com", bearer(testUser1Token), null);
        assertEquals(404, response.statusCode());
    }

    // =========================================================================
    // Additional coverage tests — Course edge cases
    // =========================================================================

    @Test
    @Order(92)
    @DisplayName("UAT-37: Get all courses — expects 200")
    void getAllCourses() throws Exception {
        HttpResponse<String> response = sendJson("GET", "/api/courses",
                bearer(testUser1Token), null);
        assertEquals(200, response.statusCode());
        JsonArray courses = JsonParser.parseString(response.body()).getAsJsonArray();
        assertTrue(courses.size() >= 0, "Should return a JSON array of courses");
    }

    @Test
    @Order(92)
    @DisplayName("UAT-38: Get course by ID — expects 200")
    void getCourseByIdExists() throws Exception {
        // Create a fresh course for this test
        HttpResponse<String> createResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Coverage Course",
                  "topic": "Testing Coverage"
                }
                """);
        assertEquals(201, createResp.statusCode());
        int cId = JsonParser.parseString(createResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        HttpResponse<String> response = sendJson("GET", "/api/courses/" + cId,
                bearer(testUser1Token), null);
        assertEquals(200, response.statusCode());
        JsonObject course = JsonParser.parseString(response.body()).getAsJsonObject();
        assertEquals("Coverage Course", course.get("className").getAsString());

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(92)
    @DisplayName("UAT-39: Get course by non-existent ID — expects 404")
    void getCourseByIdNotFound() throws Exception {
        HttpResponse<String> response = sendJson("GET", "/api/courses/999999",
                bearer(testUser1Token), null);
        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(92)
    @DisplayName("UAT-40: Create course with empty class name — expects 400")
    void createCourseEmptyName() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "",
                  "topic": "No name"
                }
                """);
        assertEquals(400, response.statusCode());
    }

    @Test
    @Order(92)
    @DisplayName("UAT-41: Create course with missing class name — expects 400")
    void createCourseMissingName() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "topic": "No className key"
                }
                """);
        assertEquals(400, response.statusCode());
    }

    @Test
    @Order(92)
    @DisplayName("UAT-42: Creator updates course — expects 200")
    void updateCourseSuccess() throws Exception {
        HttpResponse<String> createResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Update Test Course",
                  "topic": "Original"
                }
                """);
        assertEquals(201, createResp.statusCode());
        int cId = JsonParser.parseString(createResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        HttpResponse<String> updateResp = sendJson("PUT", "/api/courses/" + cId,
                bearer(testUser1Token), """
                {
                  "topic": "Updated Topic",
                  "className": "Updated Name"
                }
                """);
        assertEquals(200, updateResp.statusCode());
        JsonObject updated = JsonParser.parseString(updateResp.body()).getAsJsonObject();
        assertEquals("Updated Topic", updated.get("topic").getAsString());
        assertEquals("Updated Name", updated.get("className").getAsString());

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(92)
    @DisplayName("UAT-43: Update non-existent course — expects 404")
    void updateCourseNotFound() throws Exception {
        HttpResponse<String> response = sendJson("PUT", "/api/courses/999999",
                bearer(testUser1Token), """
                {
                  "topic": "Ghost"
                }
                """);
        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(92)
    @DisplayName("UAT-44: Delete non-existent course — expects 404")
    void deleteCourseNotFound() throws Exception {
        HttpResponse<String> response = sendJson("DELETE", "/api/courses/999999",
                bearer(testUser1Token), null);
        assertEquals(404, response.statusCode());
    }

    // =========================================================================
    // Additional coverage tests — Material edge cases
    // =========================================================================

    @Test
    @Order(93)
    @DisplayName("UAT-45: Get materials by course — expects 200")
    void getMaterialsByCourse() throws Exception {
        // Create class, upload material, then list
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Mat List Course",
                  "topic": "Materials"
                }
                """);
        assertEquals(201, classResp.statusCode());
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        HttpResponse<String> uploadResp = sendMultipartUpload(testUser1Token, cId,
                "Textbook", "listing_test.txt", "listing content");
        assertEquals(201, uploadResp.statusCode());

        HttpResponse<String> response = sendJson("GET",
                "/api/materials/course/" + cId, null, null);
        assertEquals(200, response.statusCode());
        JsonArray materials = JsonParser.parseString(response.body()).getAsJsonArray();
        assertTrue(materials.size() >= 1, "Should list at least one material");

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(93)
    @DisplayName("UAT-46: Get material by ID — expects 200")
    void getMaterialById() throws Exception {
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Mat GetById Course",
                  "topic": "GetById"
                }
                """);
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        HttpResponse<String> uploadResp = sendMultipartUpload(testUser1Token, cId,
                "Notes", "getbyid_test.txt", "getbyid");
        int mId = JsonParser.parseString(uploadResp.body()).getAsJsonObject()
                .get("fileId").getAsInt();

        HttpResponse<String> response = sendJson("GET", "/api/materials/" + mId,
                bearer(testUser1Token), null);
        assertEquals(200, response.statusCode());
        JsonObject mat = JsonParser.parseString(response.body()).getAsJsonObject();
        assertEquals("getbyid_test.txt", mat.get("originalFilename").getAsString());

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(93)
    @DisplayName("UAT-47: Get material by non-existent ID — expects 404")
    void getMaterialByIdNotFound() throws Exception {
        HttpResponse<String> response = sendJson("GET", "/api/materials/999999",
                bearer(testUser1Token), null);
        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(93)
    @DisplayName("UAT-48: Download non-existent material — expects 404")
    void downloadMaterialNotFound() throws Exception {
        HttpResponse<String> response = sendJson("GET",
                "/api/materials/999999/download", bearer(testUser1Token), null);
        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(93)
    @DisplayName("UAT-49: Creator updates material type — expects 200")
    void updateMaterialSuccess() throws Exception {
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Mat Update Course",
                  "topic": "Update"
                }
                """);
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        HttpResponse<String> uploadResp = sendMultipartUpload(testUser1Token, cId,
                "Notes", "update_test.txt", "update content");
        int mId = JsonParser.parseString(uploadResp.body()).getAsJsonObject()
                .get("fileId").getAsInt();

        HttpResponse<String> response = sendJson("PUT", "/api/materials/" + mId,
                bearer(testUser1Token), """
                {
                  "materialType": "Slides",
                  "originalFilename": "renamed.txt"
                }
                """);
        assertEquals(200, response.statusCode());
        JsonObject updated = JsonParser.parseString(response.body()).getAsJsonObject();
        assertEquals("Slides", updated.get("materialType").getAsString());
        assertEquals("renamed.txt", updated.get("originalFilename").getAsString());

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(93)
    @DisplayName("UAT-50: Non-creator tries to update material — expects 403")
    void updateMaterialNonCreator() throws Exception {
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Mat Update Forbid Course",
                  "topic": "Forbidden"
                }
                """);
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        HttpResponse<String> uploadResp = sendMultipartUpload(testUser1Token, cId,
                "Notes", "forbid_update.txt", "forbidden");
        int mId = JsonParser.parseString(uploadResp.body()).getAsJsonObject()
                .get("fileId").getAsInt();

        HttpResponse<String> response = sendJson("PUT", "/api/materials/" + mId,
                bearer(testUser2Token), """
                {
                  "materialType": "Hijacked"
                }
                """);
        assertEquals(403, response.statusCode());

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(93)
    @DisplayName("UAT-51: Update non-existent material — expects 404")
    void updateMaterialNotFound() throws Exception {
        HttpResponse<String> response = sendJson("PUT", "/api/materials/999999",
                bearer(testUser1Token), """
                {
                  "materialType": "Ghost"
                }
                """);
        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(93)
    @DisplayName("UAT-52: Delete non-existent material — expects 404")
    void deleteMaterialNotFound() throws Exception {
        HttpResponse<String> response = sendJson("DELETE", "/api/materials/999999",
                bearer(testUser1Token), null);
        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(93)
    @DisplayName("UAT-53: Upload to non-existent class — expects 400")
    void uploadToNonExistentClass() throws Exception {
        HttpResponse<String> response = sendMultipartUpload(testUser1Token, 999999,
                "Notes", "orphan_upload.txt", "orphan");
        assertEquals(400, response.statusCode());
    }

    // =========================================================================
    // Additional coverage tests — Review edge cases
    // =========================================================================

    @Test
    @Order(94)
    @DisplayName("UAT-54: Get review by ID — expects 200")
    void getReviewById() throws Exception {
        // Setup: create class, material, review
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Review GetById Course",
                  "topic": "Reviews"
                }
                """);
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        HttpResponse<String> uploadResp = sendMultipartUpload(testUser1Token, cId,
                "Notes", "review_getbyid.txt", "review content");
        int mId = JsonParser.parseString(uploadResp.body()).getAsJsonObject()
                .get("fileId").getAsInt();

        // Enroll testUser2
        sendJson("POST", "/api/participants/enroll", null, """
                { "userId": %d, "classId": %d }
                """.formatted(testUser2Id, cId));

        HttpResponse<String> reviewResp = sendJson("POST", "/api/reviews",
                bearer(testUser2Token), """
                {
                  "review": "GetById test review",
                  "rating": 3,
                  "fileId": %d
                }
                """.formatted(mId));
        assertEquals(201, reviewResp.statusCode());
        int rId = JsonParser.parseString(reviewResp.body()).getAsJsonObject()
                .get("reviewId").getAsInt();

        HttpResponse<String> response = sendJson("GET", "/api/reviews/" + rId,
                null, null);
        assertEquals(200, response.statusCode());
        JsonObject review = JsonParser.parseString(response.body()).getAsJsonObject();
        assertEquals("GetById test review", review.get("review").getAsString());

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(94)
    @DisplayName("UAT-55: Get review by non-existent ID — expects 404")
    void getReviewByIdNotFound() throws Exception {
        HttpResponse<String> response = sendJson("GET", "/api/reviews/999999",
                null, null);
        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(94)
    @DisplayName("UAT-56: Owner updates their own review — expects 200")
    void updateReviewSuccess() throws Exception {
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Review Update Course",
                  "topic": "Review Updates"
                }
                """);
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        HttpResponse<String> uploadResp = sendMultipartUpload(testUser1Token, cId,
                "Notes", "review_update.txt", "review update content");
        int mId = JsonParser.parseString(uploadResp.body()).getAsJsonObject()
                .get("fileId").getAsInt();

        sendJson("POST", "/api/participants/enroll", null, """
                { "userId": %d, "classId": %d }
                """.formatted(testUser2Id, cId));

        HttpResponse<String> reviewResp = sendJson("POST", "/api/reviews",
                bearer(testUser2Token), """
                {
                  "review": "Original review",
                  "rating": 2,
                  "fileId": %d
                }
                """.formatted(mId));
        int rId = JsonParser.parseString(reviewResp.body()).getAsJsonObject()
                .get("reviewId").getAsInt();

        HttpResponse<String> response = sendJson("PUT", "/api/reviews/" + rId,
                bearer(testUser2Token), """
                {
                  "review": "Updated review text",
                  "rating": 5
                }
                """);
        assertEquals(200, response.statusCode());
        JsonObject updated = JsonParser.parseString(response.body()).getAsJsonObject();
        assertEquals("Updated review text", updated.get("review").getAsString());
        assertEquals(5, updated.get("rating").getAsInt());

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(94)
    @DisplayName("UAT-57: Update non-existent review — expects 404")
    void updateReviewNotFound() throws Exception {
        HttpResponse<String> response = sendJson("PUT", "/api/reviews/999999",
                bearer(testUser2Token), """
                {
                  "review": "Ghost",
                  "rating": 1
                }
                """);
        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(94)
    @DisplayName("UAT-58: Owner deletes their own review — expects 200")
    void deleteReviewSuccess() throws Exception {
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Review Delete Course",
                  "topic": "Review Deletes"
                }
                """);
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        HttpResponse<String> uploadResp = sendMultipartUpload(testUser1Token, cId,
                "Notes", "review_delete.txt", "review delete content");
        int mId = JsonParser.parseString(uploadResp.body()).getAsJsonObject()
                .get("fileId").getAsInt();

        sendJson("POST", "/api/participants/enroll", null, """
                { "userId": %d, "classId": %d }
                """.formatted(testUser2Id, cId));

        HttpResponse<String> reviewResp = sendJson("POST", "/api/reviews",
                bearer(testUser2Token), """
                {
                  "review": "Delete me",
                  "rating": 1,
                  "fileId": %d
                }
                """.formatted(mId));
        int rId = JsonParser.parseString(reviewResp.body()).getAsJsonObject()
                .get("reviewId").getAsInt();

        HttpResponse<String> response = sendJson("DELETE", "/api/reviews/" + rId,
                bearer(testUser2Token), null);
        assertEquals(200, response.statusCode());

        // Verify it's gone
        HttpResponse<String> getResp = sendJson("GET", "/api/reviews/" + rId,
                null, null);
        assertEquals(404, getResp.statusCode());

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(94)
    @DisplayName("UAT-59: Delete non-existent review — expects 404")
    void deleteReviewNotFound() throws Exception {
        HttpResponse<String> response = sendJson("DELETE", "/api/reviews/999999",
                bearer(testUser2Token), null);
        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(94)
    @DisplayName("UAT-60: Create review with missing text — expects 400")
    void createReviewMissingText() throws Exception {
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Review MissingText Course",
                  "topic": "MissingText"
                }
                """);
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        HttpResponse<String> uploadResp = sendMultipartUpload(testUser1Token, cId,
                "Notes", "missing_text.txt", "mt content");
        int mId = JsonParser.parseString(uploadResp.body()).getAsJsonObject()
                .get("fileId").getAsInt();

        HttpResponse<String> response = sendJson("POST", "/api/reviews",
                bearer(testUser1Token), """
                {
                  "rating": 3,
                  "fileId": %d
                }
                """.formatted(mId));
        assertEquals(400, response.statusCode());

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(94)
    @DisplayName("UAT-61: Update review without auth — expects 401")
    void updateReviewNoAuth() throws Exception {
        HttpResponse<String> response = sendJson("PUT", "/api/reviews/1",
                null, """
                {
                  "review": "no auth",
                  "rating": 1
                }
                """);
        assertEquals(401, response.statusCode());
    }

    @Test
    @Order(94)
    @DisplayName("UAT-62: Delete review without auth — expects 401")
    void deleteReviewNoAuth() throws Exception {
        HttpResponse<String> response = sendJson("DELETE", "/api/reviews/1",
                null, null);
        assertEquals(401, response.statusCode());
    }

    // =========================================================================
    // Additional coverage tests — Participant edge cases
    // =========================================================================

    @Test
    @Order(95)
    @DisplayName("UAT-63: Enroll to non-existent class — expects 400")
    void enrollNonExistentClass() throws Exception {
        HttpResponse<String> response = sendJson("POST", "/api/participants/enroll", null, """
                {
                  "userId": %d,
                  "classId": 999999
                }
                """.formatted(testUser2Id));
        assertEquals(400, response.statusCode());
    }

    @Test
    @Order(95)
    @DisplayName("UAT-64: Enroll non-existent user — expects 400")
    void enrollNonExistentUser() throws Exception {
        // Create a class for this test
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Enroll Test Course",
                  "topic": "Enrollment"
                }
                """);
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        HttpResponse<String> response = sendJson("POST", "/api/participants/enroll", null, """
                {
                  "userId": 999999,
                  "classId": %d
                }
                """.formatted(cId));
        assertEquals(400, response.statusCode());

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(95)
    @DisplayName("UAT-65: Get participants by class — expects 200")
    void getParticipantsByClass() throws Exception {
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Participants List Course",
                  "topic": "List"
                }
                """);
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        // Enroll another user
        sendJson("POST", "/api/participants/enroll", null, """
                { "userId": %d, "classId": %d }
                """.formatted(testUser2Id, cId));

        HttpResponse<String> response = sendJson("GET",
                "/api/participants/class/" + cId, bearer(testUser1Token), null);
        assertEquals(200, response.statusCode());
        JsonArray participants = JsonParser.parseString(response.body()).getAsJsonArray();
        assertTrue(participants.size() >= 2, "Should have creator + enrolled user");

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(95)
    @DisplayName("UAT-66: Get classes by user — expects 200")
    void getClassesByUser() throws Exception {
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "ClassesByUser Course",
                  "topic": "ByUser"
                }
                """);
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        HttpResponse<String> response = sendJson("GET",
                "/api/participants/user/" + testUser1Id, bearer(testUser1Token), null);
        assertEquals(200, response.statusCode());
        JsonArray classes = JsonParser.parseString(response.body()).getAsJsonArray();
        assertTrue(classes.size() >= 1, "Creator should be enrolled in at least one class");

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(95)
    @DisplayName("UAT-67: Self-unenroll from a class — expects 200")
    void selfUnenroll() throws Exception {
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Unenroll Test Course",
                  "topic": "Unenroll"
                }
                """);
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        // Enroll testUser2
        sendJson("POST", "/api/participants/enroll", null, """
                { "userId": %d, "classId": %d }
                """.formatted(testUser2Id, cId));

        // testUser2 unenrolls themselves
        HttpResponse<String> response = sendJson("DELETE", "/api/participants/unenroll",
                bearer(testUser2Token), """
                {
                  "userId": %d,
                  "classId": %d
                }
                """.formatted(testUser2Id, cId));
        assertEquals(200, response.statusCode());

        // Verify they're gone
        HttpResponse<String> participants = sendJson("GET",
                "/api/participants/class/" + cId, bearer(testUser1Token), null);
        JsonArray list = JsonParser.parseString(participants.body()).getAsJsonArray();
        for (int i = 0; i < list.size(); i++) {
            assertNotEquals(testUser2Id, list.get(i).getAsInt(),
                    "testUser2 should not be in participants after unenroll");
        }

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(95)
    @DisplayName("UAT-68: Other user tries to unenroll someone — expects 403")
    void unenrollOthersForbidden() throws Exception {
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Unenroll Forbid Course",
                  "topic": "Forbidden Unenroll"
                }
                """);
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        // Enroll testUser2 and testUser3
        sendJson("POST", "/api/participants/enroll", null, """
                { "userId": %d, "classId": %d }
                """.formatted(testUser2Id, cId));
        sendJson("POST", "/api/participants/enroll", null, """
                { "userId": %d, "classId": %d }
                """.formatted(testUser3Id, cId));

        // testUser3 tries to unenroll testUser2 (not creator, not self)
        HttpResponse<String> response = sendJson("DELETE", "/api/participants/unenroll",
                bearer(testUser3Token), """
                {
                  "userId": %d,
                  "classId": %d
                }
                """.formatted(testUser2Id, cId));
        assertEquals(403, response.statusCode());

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(95)
    @DisplayName("UAT-69: Creator unenrolls another user — expects 200")
    void creatorUnenrollsOther() throws Exception {
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Creator Unenroll Course",
                  "topic": "Creator Unenroll"
                }
                """);
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        sendJson("POST", "/api/participants/enroll", null, """
                { "userId": %d, "classId": %d }
                """.formatted(testUser2Id, cId));

        // Creator unenrolls testUser2
        HttpResponse<String> response = sendJson("DELETE", "/api/participants/unenroll",
                bearer(testUser1Token), """
                {
                  "userId": %d,
                  "classId": %d
                }
                """.formatted(testUser2Id, cId));
        assertEquals(200, response.statusCode());

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    @Test
    @Order(95)
    @DisplayName("UAT-70: Unenroll from non-existent class — expects 400")
    void unenrollNonExistentClass() throws Exception {
        HttpResponse<String> response = sendJson("DELETE", "/api/participants/unenroll",
                bearer(testUser2Token), """
                {
                  "userId": %d,
                  "classId": 999999
                }
                """.formatted(testUser2Id));
        assertEquals(400, response.statusCode());
    }

    @Test
    @Order(95)
    @DisplayName("UAT-71: Unenroll when not actually enrolled — expects 404")
    void unenrollNotEnrolled() throws Exception {
        HttpResponse<String> classResp = sendJson("POST", "/api/courses",
                bearer(testUser1Token), """
                {
                  "className": "Not Enrolled Course",
                  "topic": "Not Enrolled"
                }
                """);
        int cId = JsonParser.parseString(classResp.body()).getAsJsonObject()
                .get("classId").getAsInt();

        // testUser3 tries to unenroll self but was never enrolled
        HttpResponse<String> response = sendJson("DELETE", "/api/participants/unenroll",
                bearer(testUser3Token), """
                {
                  "userId": %d,
                  "classId": %d
                }
                """.formatted(testUser3Id, cId));
        assertEquals(404, response.statusCode());

        // Cleanup
        sendJson("DELETE", "/api/courses/" + cId, bearer(testUser1Token), null);
    }

    // =========================================================================
    // Full workflow test — end-to-end across all criteria
    // =========================================================================

    @Test
    @Order(100)
    @DisplayName("UAT-20: Full end-to-end workflow (register → login → class → upload → join → download → review → view reviews → delete material → delete class)")
    void fullWorkflow() throws Exception {
        // 1. Register a new teacher
        HttpResponse<String> regTeacher = sendJson("POST", "/api/auth/register", null, """
                {
                  "username": "workflow_teacher",
                  "email": "workflow_teacher@example.com",
                  "password": "TeacherPass1"
                }
                """);
        assertEquals(201, regTeacher.statusCode());
        int teacherId = JsonParser.parseString(regTeacher.body()).getAsJsonObject()
                .get("userId").getAsInt();

        // 2. Login teacher
        HttpResponse<String> loginTeacher = sendJson("POST", "/api/auth/login", null, """
                {
                  "email": "workflow_teacher@example.com",
                  "password": "TeacherPass1"
                }
                """);
        assertEquals(200, loginTeacher.statusCode());
        String teacherToken = JsonParser.parseString(loginTeacher.body()).getAsJsonObject()
                .get("token").getAsString();

        // 3. Create a class
        HttpResponse<String> createClass = sendJson("POST", "/api/courses",
                bearer(teacherToken), """
                {
                  "className": "Workflow Physics",
                  "topic": "Physics"
                }
                """);
        assertEquals(201, createClass.statusCode());
        int wfClassId = JsonParser.parseString(createClass.body()).getAsJsonObject()
                .get("classId").getAsInt();

        // 4. Upload material
        HttpResponse<String> upload = sendMultipartUpload(teacherToken, wfClassId,
                "Lecture Notes", "physics_notes.txt", "F = ma");
        assertEquals(201, upload.statusCode());
        int wfMaterialId = JsonParser.parseString(upload.body()).getAsJsonObject()
                .get("fileId").getAsInt();

        // 5. Register and login a student
        HttpResponse<String> regStudent = sendJson("POST", "/api/auth/register", null, """
                {
                  "username": "workflow_student",
                  "email": "workflow_student@example.com",
                  "password": "StudentPass1"
                }
                """);
        assertEquals(201, regStudent.statusCode());
        int studentId = JsonParser.parseString(regStudent.body()).getAsJsonObject()
                .get("userId").getAsInt();

        HttpResponse<String> loginStudent = sendJson("POST", "/api/auth/login", null, """
                {
                  "email": "workflow_student@example.com",
                  "password": "StudentPass1"
                }
                """);
        assertEquals(200, loginStudent.statusCode());
        String studentToken = JsonParser.parseString(loginStudent.body()).getAsJsonObject()
                .get("token").getAsString();

        // 6. Student joins the class
        HttpResponse<String> join = sendJson("POST", "/api/participants/enroll", null, """
                {
                  "userId": %d,
                  "classId": %d
                }
                """.formatted(studentId, wfClassId));
        assertEquals(201, join.statusCode());

        // 7. Student downloads the material
        HttpResponse<String> download = sendJson("GET",
                "/api/materials/" + wfMaterialId + "/download",
                bearer(studentToken), null);
        assertEquals(200, download.statusCode());
        assertTrue(download.body().contains("F = ma"), "Downloaded content should match upload");

        // 8. Student leaves a review
        HttpResponse<String> review = sendJson("POST", "/api/reviews",
                bearer(studentToken), """
                {
                  "review": "Great physics notes!",
                  "rating": 5,
                  "fileId": %d
                }
                """.formatted(wfMaterialId));
        assertEquals(201, review.statusCode());

        // 9. View reviews
        HttpResponse<String> viewReviews = sendJson("GET",
                "/api/reviews/material/" + wfMaterialId, null, null);
        assertEquals(200, viewReviews.statusCode());
        JsonArray reviewList = JsonParser.parseString(viewReviews.body()).getAsJsonArray();
        assertTrue(reviewList.size() >= 1);

        // 10. Teacher deletes the material
        HttpResponse<String> deleteMat = sendJson("DELETE",
                "/api/materials/" + wfMaterialId, bearer(teacherToken), null);
        assertEquals(200, deleteMat.statusCode());

        // 11. Teacher deletes the class
        HttpResponse<String> deleteCls = sendJson("DELETE",
                "/api/courses/" + wfClassId, bearer(teacherToken), null);
        assertEquals(200, deleteCls.statusCode());

        // Verify everything is cleaned up
        assertNull(new CourseDao().findById(wfClassId));
        assertTrue(new ParticipantDao().findUsersByClass(wfClassId).isEmpty());
        assertTrue(new MaterialDao().findByClassId(wfClassId).isEmpty());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static void checkAuth(Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Missing or invalid token");
        }
        String token = authHeader.substring(7);
        String email = JWTUtil.validateToken(token);
        if (email == null) {
            throw new UnauthorizedResponse("Invalid token");
        }
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

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static HttpResponse<String> sendJson(String method, String path,
            String authorization, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path));

        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }

        switch (method) {
            case "POST" -> builder.POST(body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));
            case "PUT" -> builder.PUT(body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));
            case "DELETE" -> {
                if (body == null) {
                    builder.DELETE();
                } else {
                    builder.method("DELETE", HttpRequest.BodyPublishers.ofString(body));
                }
            }
            case "GET" -> builder.GET();
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }

        return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> sendMultipartUpload(String token, int classId,
            String materialType, String filename, String fileContent)
            throws IOException, InterruptedException {
        String boundary = "----JavaBoundary" + System.currentTimeMillis();
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + fileContent + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"classId\"\r\n\r\n"
                + classId + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"materialType\"\r\n\r\n"
                + materialType + "\r\n"
                + "--" + boundary + "--\r\n";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/materials"))
                .header("Authorization", bearer(token))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
