package com.example.app;

import com.example.otp.controllers.*;
import com.example.otp.db.DatabaseInitializer;
import com.example.otp.util.JWTUtil;
import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;

import javafx.application.Application;
import javafx.stage.Stage;
import java.util.Locale;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        LocaleManager.setLocale(Locale.ENGLISH);
        stage.setMaximized(true);
        SceneManager.setPrimaryStage(stage);
        SceneManager.loadMain();
    }

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        DatabaseInitializer.initializeDatabase();

        new Thread(() -> {
            UserController userController = new UserController();
            CourseController courseController = new CourseController();
            MaterialController materialController = new MaterialController();
            ReviewController reviewController = new ReviewController();
            ParticipantController participantController = new ParticipantController();

            Javalin app = Javalin.create(config -> {
                config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
            }).start(7700);

            // Auth middleware for protected routes
            String[] protectedPaths = {
                "/api/users", "/api/users/{id}",
                "/api/courses", "/api/courses/{id}",
                "/api/materials", "/api/materials/{id}", "/api/materials/{id}/download",
                "/api/participants/unenroll", "/api/participants/class/{classId}", "/api/participants/user/{userId}",
                "/api/auth/change-password"
            };
            for (String path : protectedPaths) {
                app.before(path, ctx -> checkAuth(ctx));
            }

            // Auth routes
            app.post("/api/auth/register", userController::register);
            app.post("/api/auth/login", userController::login);
            app.put("/api/auth/change-password", userController::changePassword);

            // User routes
            app.get("/api/users", userController::getAllUsers);
            app.get("/api/users/{id}", userController::getUserById);
            app.get("/api/users/by-email/{email}", userController::getUserByEmail);

            // Course routes
            app.get("/api/courses", courseController::getAllCourses);
            app.get("/api/courses/{id}", courseController::getCourseById);
            app.post("/api/courses", courseController::createCourse);
            app.put("/api/courses/{id}", courseController::updateCourse);
            app.delete("/api/courses/{id}", courseController::deleteCourse);

            // Material routes
            app.get("/api/materials/course/{classId}", materialController::getMaterialsByCourse);
            app.get("/api/materials/{id}", materialController::getMaterialById);
            app.get("/api/materials/{id}/download", materialController::downloadMaterial);
            app.post("/api/materials", materialController::uploadMaterial);
            app.put("/api/materials/{id}", materialController::updateMaterial);
            app.delete("/api/materials/{id}", materialController::deleteMaterial);

            // Review routes
            app.get("/api/reviews/material/{fileId}", reviewController::getReviewsByMaterial);
            app.get("/api/reviews/{id}", reviewController::getReviewById);
            app.post("/api/reviews", reviewController::createReview);
            app.put("/api/reviews/{id}", reviewController::updateReview);
            app.delete("/api/reviews/{id}", reviewController::deleteReview);

            // Participant routes
            app.post("/api/participants/enroll", participantController::enrollUser);
            app.delete("/api/participants/unenroll", participantController::unenrollUser);
            app.get("/api/participants/class/{classId}", participantController::getUsersByClass);
            app.get("/api/participants/user/{userId}", participantController::getClassesByUser);
        }).start();

        launch(args);
    }

    private static void checkAuth(io.javalin.http.Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Missing or invalid token");
        }
        String email = JWTUtil.validateToken(authHeader.substring(7));
        if (email == null) {
            throw new UnauthorizedResponse("Invalid token");
        }
        ctx.attribute("email", email);
    }
}