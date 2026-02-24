package com.example.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {
    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void loadLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/login.fxml"));
            Scene scene = new Scene(loader.load());
            primaryStage.setTitle("Learning Platform - Login");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Error loading login scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadDashboard(String userType, String token, String username) {
        try {
            String normalizedUserType = (userType == null || userType.isBlank()) ? "student" : userType;

            if ("admin".equals(normalizedUserType)) {
                FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/admin.fxml"));
                Scene scene = new Scene(loader.load());
                AdminController controller = loader.getController();
                controller.setAuthToken(token);
                primaryStage.setTitle("Learning Platform - Admin Dashboard");
                primaryStage.setScene(scene);
                primaryStage.show();
            } else {
                FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/dashboard.fxml"));
                Scene scene = new Scene(loader.load());
                DashboardController controller = loader.getController();
                controller.setUserInfo(normalizedUserType, username);
                primaryStage.setTitle("Learning Platform - " + capitalize(normalizedUserType) + " Dashboard");
                primaryStage.setScene(scene);
                primaryStage.show();
            }
        } catch (Exception e) {
            System.err.println("Error loading dashboard scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
