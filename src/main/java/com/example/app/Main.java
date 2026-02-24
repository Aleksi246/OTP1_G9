// src/main/java/com/example/app/Main.java
package com.example.app;

import com.example.otp.controllers.*;
import com.example.otp.util.JWTUtil;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        SceneManager.setPrimaryStage(stage);
        SceneManager.loadLogin();
    }

    public static void main(String[] args) {
        // Start Javalin server in another thread to avoid blocking JavaFX application
        new Thread(() -> {

            // Initialize controllers
            UserController userController = new UserController();
            CourseController courseController = new CourseController();
            MaterialController materialController = new MaterialController();
            ReviewController reviewController = new ReviewController();
            ParticipantController participantController = new ParticipantController();

            // Start Javalin server
            Javalin app = Javalin.create(config -> {
                config.plugins.enableCors(cors -> {
                    cors.add(it -> {
                        it.anyHost();
                    });
                });
            }).start(7700);

            // Authentication middleware - only for protected routes
            app.before("/api/users", ctx -> checkAuth(ctx));
            app.before("/api/users/{id}", ctx -> checkAuth(ctx));
            app.before("/api/courses", ctx -> checkAuth(ctx));
            app.before("/api/courses/{id}", ctx -> checkAuth(ctx));
            app.before("/api/materials", ctx -> checkAuth(ctx));
            app.before("/api/materials/{id}", ctx -> checkAuth(ctx));
            app.before("/api/materials/{id}/download", ctx -> checkAuth(ctx));
            app.before("/api/participants", ctx -> checkAuth(ctx));
            app.before("/api/participants/enroll", ctx -> checkAuth(ctx));
            app.before("/api/participants/unenroll", ctx -> checkAuth(ctx));
            app.before("/api/participants/class/{classId}", ctx -> checkAuth(ctx));
            app.before("/api/participants/user/{userId}", ctx -> checkAuth(ctx));
            app.before("/api/admin/create-teacher", ctx -> checkAuth(ctx));

            app.post("/api/auth/register", userController::register);
            app.post("/api/auth/login", userController::login);

            // Admin routes
            app.post("/api/admin/create-teacher", userController::createTeacher);

            // User routes
            app.get("/api/users", userController::getAllUsers);
            app.get("/api/users/{id}", userController::getUserById);

            // Course routes
            app.get("/api/courses", courseController::getAllCourses);
            app.get("/api/courses/{id}", courseController::getCourseById);
            app.post("/api/courses", courseController::createCourse);
            app.put("/api/courses/{id}", courseController::updateCourse);
            app.delete("/api/courses/{id}", courseController::deleteCourse);

            // Material routes
            app.get("/api/materials/course/{classId}", materialController::getMaterialsByCourse);
            app.get("/api/materials/{id}", materialController::getMaterialById);
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

            System.out.println("Server started on port 7700");
        }).start();

        launch(args);
    }

    private static void checkAuth(io.javalin.http.Context ctx) {
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
}