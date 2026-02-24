package com.example.otp.controllers;

import com.example.otp.dao.CourseDao;
import com.example.otp.dao.MaterialDao;
import com.example.otp.dao.ParticipantDao;
import com.example.otp.dao.ReviewDao;
import com.example.otp.dao.UserDao;
import com.example.otp.db.Database;
import com.example.otp.model.Course;
import com.example.otp.model.Material;
import com.example.otp.model.Review;
import com.example.otp.model.User;
import com.example.otp.util.BCryptUtil;
import com.example.otp.util.JWTUtil;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApiEndpointsIntegrationTest {

    private static Javalin app;
    private static String baseUrl;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private int adminId;
    private int teacherId;
    private int teacher2Id;
    private int studentId;
    private int student2Id;
    private int classId;
    private int class2Id;
    private int materialId;
    private int reviewId;

    @BeforeAll
    static void startServer() {
        UserController userController = new UserController();
        CourseController courseController = new CourseController();
        MaterialController materialController = new MaterialController();
        ReviewController reviewController = new ReviewController();
        ParticipantController participantController = new ParticipantController();

        app = Javalin.create(config -> config.plugins.enableCors(cors -> cors.add(it -> it.anyHost())));

        app.before("/api/users", ApiEndpointsIntegrationTest::checkAuth);
        app.before("/api/users/{id}", ApiEndpointsIntegrationTest::checkAuth);
        app.before("/api/courses", ApiEndpointsIntegrationTest::checkAuth);
        app.before("/api/courses/{id}", ApiEndpointsIntegrationTest::checkAuth);
        app.before("/api/materials", ApiEndpointsIntegrationTest::checkAuth);
        app.before("/api/materials/{id}", ApiEndpointsIntegrationTest::checkAuth);
        app.before("/api/materials/{id}/download", ApiEndpointsIntegrationTest::checkAuth);
        app.before("/api/participants", ApiEndpointsIntegrationTest::checkAuth);
        app.before("/api/participants/enroll", ApiEndpointsIntegrationTest::checkAuth);
        app.before("/api/participants/unenroll", ApiEndpointsIntegrationTest::checkAuth);
        app.before("/api/participants/class/{classId}", ApiEndpointsIntegrationTest::checkAuth);
        app.before("/api/participants/user/{userId}", ApiEndpointsIntegrationTest::checkAuth);
        app.before("/api/admin/create-teacher", ApiEndpointsIntegrationTest::checkAuth);

        app.post("/api/auth/register", userController::register);
        app.post("/api/auth/login", userController::login);

        app.post("/api/admin/create-teacher", userController::createTeacher);

        app.get("/api/users", userController::getAllUsers);
        app.get("/api/users/{id}", userController::getUserById);

        app.get("/api/courses", courseController::getAllCourses);
        app.get("/api/courses/{id}", courseController::getCourseById);
        app.post("/api/courses", courseController::createCourse);
        app.put("/api/courses/{id}", courseController::updateCourse);
        app.delete("/api/courses/{id}", courseController::deleteCourse);

        app.get("/api/materials/course/{classId}", materialController::getMaterialsByCourse);
        app.get("/api/materials/{id}", materialController::getMaterialById);
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
    }

    @AfterAll
    static void stopServer() {
        if (app != null) {
            app.stop();
        }
    }

    @BeforeEach
    void resetAndSeed() throws Exception {
        clearAllTables();

        UserDao userDao = new UserDao();
        CourseDao courseDao = new CourseDao();
        ParticipantDao participantDao = new ParticipantDao();
        MaterialDao materialDao = new MaterialDao();
        ReviewDao reviewDao = new ReviewDao();

        User admin = createUser(userDao, "admin_seed", "adminPass", "admin");
        User teacher = createUser(userDao, "teacher_seed", "teacherPass", "teacher");
        User teacher2 = createUser(userDao, "teacher2_seed", "teacherPass", "teacher");
        User student = createUser(userDao, "student_seed", "studentPass", "student");
        User student2 = createUser(userDao, "student2_seed", "studentPass", "student");

        adminId = admin.getUserId();
        teacherId = teacher.getUserId();
        teacher2Id = teacher2.getUserId();
        studentId = student.getUserId();
        student2Id = student2.getUserId();

        Course c1 = new Course();
        c1.setClassName("Seed Class 1");
        c1.setTopic("Topic 1");
        c1 = courseDao.create(c1);
        classId = c1.getClassId();

        Course c2 = new Course();
        c2.setClassName("Seed Class 2");
        c2.setTopic("Topic 2");
        c2 = courseDao.create(c2);
        class2Id = c2.getClassId();

        participantDao.addParticipant(teacherId, classId);
        participantDao.addParticipant(studentId, classId);

        Material material = new Material();
        material.setOriginalFilename("seed.txt");
        material.setStoredFilename("seed-file-id.txt");
        material.setFilepath("uploads/seed-file-id.txt");
        material.setMaterialType("lecture_notes");
        material.setClassId(classId);
        material.setUserId(teacherId);
        material = materialDao.create(material);
        materialId = material.getFileId();

        Review review = new Review();
        review.setReview("Seed review");
        review.setRating(4);
        review.setFileId(materialId);
        review.setUserId(studentId);
        review = reviewDao.create(review);
        reviewId = review.getReviewId();
    }

    @Test
    void authEndpoints_registerAndLogin() throws Exception {
        HttpResponse<String> register = sendJson("POST", "/api/auth/register", null, """
                {
                  "username": "new_student",
                  "password": "newpass"
                }
                """);
        assertEquals(201, register.statusCode());

        HttpResponse<String> login = sendJson("POST", "/api/auth/login", null, """
                {
                  "username": "new_student",
                  "password": "newpass"
                }
                """);
        assertEquals(200, login.statusCode());
        assertTrue(login.body().contains("token"));
    }

    @Test
    void userEndpoints_getAllAndGetById_requireToken() throws Exception {
        HttpResponse<String> noToken = sendJson("GET", "/api/users", null, null);
        assertEquals(401, noToken.statusCode());

        String teacherToken = tokenFor("teacher_seed");
        HttpResponse<String> allUsers = sendJson("GET", "/api/users", bearer(teacherToken), null);
        assertEquals(200, allUsers.statusCode());

        HttpResponse<String> byId = sendJson("GET", "/api/users/" + studentId, bearer(teacherToken), null);
        assertEquals(200, byId.statusCode());
    }

    @Test
    void adminEndpoint_createTeacher_adminOnly() throws Exception {
        String studentToken = tokenFor("student_seed");
        HttpResponse<String> forbidden = sendJson("POST", "/api/admin/create-teacher", bearer(studentToken), """
                {
                  "username": "teacher_forbidden",
                  "password": "pass123"
                }
                """);
        assertEquals(403, forbidden.statusCode());

        String adminToken = tokenFor("admin_seed");
        HttpResponse<String> created = sendJson("POST", "/api/admin/create-teacher", bearer(adminToken), """
                {
                  "username": "teacher_created",
                  "password": "pass123"
                }
                """);
        assertEquals(201, created.statusCode());
    }

    @Test
    void courseEndpoints_allRoutesCovered() throws Exception {
        String teacherToken = tokenFor("teacher_seed");
        String teacher2Token = tokenFor("teacher2_seed");

        HttpResponse<String> getAll = sendJson("GET", "/api/courses", bearer(teacherToken), null);
        assertEquals(200, getAll.statusCode());

        HttpResponse<String> getById = sendJson("GET", "/api/courses/" + classId, bearer(teacherToken), null);
        assertEquals(200, getById.statusCode());

        HttpResponse<String> create = sendJson("POST", "/api/courses", bearer(teacherToken), """
                {
                  "className": "Created By Test",
                  "topic": "API"
                }
                """);
        assertEquals(201, create.statusCode());

        HttpResponse<String> updateForbidden = sendJson("PUT", "/api/courses/" + classId, bearer(teacher2Token), """
                {
                  "topic": "Should fail"
                }
                """);
        assertEquals(403, updateForbidden.statusCode());

        HttpResponse<String> update = sendJson("PUT", "/api/courses/" + classId, bearer(teacherToken), """
                {
                  "topic": "Updated Topic"
                }
                """);
        assertEquals(200, update.statusCode());

        HttpResponse<String> delete = sendJson("DELETE", "/api/courses/" + classId, bearer(teacherToken), null);
        assertEquals(200, delete.statusCode());
    }

    @Test
    void materialEndpoints_allRoutesCovered() throws Exception {
        String teacherToken = tokenFor("teacher_seed");

        HttpResponse<String> getByCoursePublic = sendJson("GET", "/api/materials/course/" + classId, null, null);
        assertEquals(200, getByCoursePublic.statusCode());

        HttpResponse<String> getByIdNoToken = sendJson("GET", "/api/materials/" + materialId, null, null);
        assertEquals(401, getByIdNoToken.statusCode());

        HttpResponse<String> getById = sendJson("GET", "/api/materials/" + materialId, bearer(teacherToken), null);
        assertEquals(200, getById.statusCode());

        HttpResponse<String> upload = sendMultipartUpload(teacherToken, classId, teacherId, "lecture_notes", "upload-test.txt", "hello");
        assertEquals(201, upload.statusCode());

        HttpResponse<String> update = sendJson("PUT", "/api/materials/" + materialId, bearer(teacherToken), """
                {
                  "materialType": "slides"
                }
                """);
        assertEquals(200, update.statusCode());

        HttpResponse<String> delete = sendJson("DELETE", "/api/materials/" + materialId, bearer(teacherToken), null);
        assertEquals(200, delete.statusCode());
    }

    @Test
    void reviewEndpoints_allRoutesCoveredWithOwnership() throws Exception {
        String studentToken = tokenFor("student_seed");
        String student2Token = tokenFor("student2_seed");

        HttpResponse<String> getByMaterialPublic = sendJson("GET", "/api/reviews/material/" + materialId, null, null);
        assertEquals(200, getByMaterialPublic.statusCode());

        HttpResponse<String> getByIdPublic = sendJson("GET", "/api/reviews/" + reviewId, null, null);
        assertEquals(200, getByIdPublic.statusCode());

        HttpResponse<String> createNoToken = sendJson("POST", "/api/reviews", null, """
                {
                  "review": "No token",
                  "rating": 3,
                  "fileId": %d
                }
                """.formatted(materialId));
        assertEquals(401, createNoToken.statusCode());

        HttpResponse<String> create = sendJson("POST", "/api/reviews", bearer(studentToken), """
                {
                  "review": "Created by owner",
                  "rating": 5,
                  "fileId": %d
                }
                """.formatted(materialId));
        assertEquals(201, create.statusCode());

        HttpResponse<String> updateWrongUser = sendJson("PUT", "/api/reviews/" + reviewId, bearer(student2Token), """
                {
                  "review": "Should fail",
                  "rating": 1
                }
                """);
        assertEquals(403, updateWrongUser.statusCode());

        HttpResponse<String> updateOwner = sendJson("PUT", "/api/reviews/" + reviewId, bearer(studentToken), """
                {
                  "review": "Updated by owner",
                  "rating": 5
                }
                """);
        assertEquals(200, updateOwner.statusCode());

        HttpResponse<String> deleteWrongUser = sendJson("DELETE", "/api/reviews/" + reviewId, bearer(student2Token), null);
        assertEquals(403, deleteWrongUser.statusCode());

        HttpResponse<String> deleteOwner = sendJson("DELETE", "/api/reviews/" + reviewId, bearer(studentToken), null);
        assertEquals(200, deleteOwner.statusCode());
    }

    @Test
    void participantEndpoints_allRoutesCovered() throws Exception {
        String teacherToken = tokenFor("teacher_seed");
        String studentToken = tokenFor("student_seed");

        HttpResponse<String> enrollForbidden = sendJson("POST", "/api/participants/enroll", bearer(studentToken), """
                {
                  "userId": %d,
                  "classId": %d
                }
                """.formatted(student2Id, class2Id));
        assertEquals(403, enrollForbidden.statusCode());

        HttpResponse<String> enroll = sendJson("POST", "/api/participants/enroll", bearer(teacherToken), """
                {
                  "userId": %d,
                  "classId": %d
                }
                """.formatted(student2Id, class2Id));
        assertEquals(201, enroll.statusCode());

        HttpResponse<String> getByClass = sendJson("GET", "/api/participants/class/" + class2Id, bearer(teacherToken), null);
        assertEquals(200, getByClass.statusCode());

        HttpResponse<String> getByUser = sendJson("GET", "/api/participants/user/" + student2Id, bearer(teacherToken), null);
        assertEquals(200, getByUser.statusCode());

        HttpResponse<String> unenroll = sendJson("DELETE", "/api/participants/unenroll", bearer(teacherToken), """
                {
                  "userId": %d,
                  "classId": %d
                }
                """.formatted(student2Id, class2Id));
        assertEquals(200, unenroll.statusCode());
    }

    private static void checkAuth(Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Missing or invalid token");
        }
        String token = authHeader.substring(7);
        String username = JWTUtil.validateToken(token);
        if (username == null) {
            throw new UnauthorizedResponse("Invalid token");
        }
        ctx.attribute("username", username);
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

    private static User createUser(UserDao dao, String username, String password, String userType) throws Exception {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(BCryptUtil.hashPassword(password));
        user.setUserType(userType);
        return dao.create(user);
    }

    private static String tokenFor(String username) {
        return JWTUtil.generateToken(username);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static HttpResponse<String> sendJson(String method, String path, String authorization, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path));

        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }

        switch (method) {
            case "POST" -> builder.POST(body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            case "PUT" -> builder.PUT(body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
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

    private static HttpResponse<String> sendMultipartUpload(String token, int classId, int userId, String materialType, String filename, String fileContent) throws IOException, InterruptedException {
        String boundary = "----JavaBoundary" + System.currentTimeMillis();
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + fileContent + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"classId\"\r\n\r\n"
                + classId + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"userId\"\r\n\r\n"
                + userId + "\r\n"
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
