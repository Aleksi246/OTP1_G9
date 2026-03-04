package com.example.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class SceneManager {
    private static Stage primaryStage;
    private static List<String> navigationHistory = new ArrayList<>();
    private static int currentIndex = -1;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    private static void addToHistory(String sceneName) {
        // Remove any forward history if we're navigating to a new scene
        if (currentIndex < navigationHistory.size() - 1) {
            navigationHistory = new ArrayList<>(navigationHistory.subList(0, currentIndex + 1));
        }
        navigationHistory.add(sceneName);
        currentIndex++;
    }

    public static boolean canGoBack() {
        return currentIndex > 0;
    }

    public static boolean canGoForward() {
        return currentIndex < navigationHistory.size() - 1;
    }

    public static void goBack() {
        if (canGoBack()) {
            currentIndex--;
            loadSceneByName(navigationHistory.get(currentIndex), false);
        }
    }

    public static void goForward() {
        if (canGoForward()) {
            currentIndex++;
            loadSceneByName(navigationHistory.get(currentIndex), false);
        }
    }

    private static void loadSceneByName(String sceneName, boolean addToHistory) {
        switch (sceneName) {
            case "main":
                loadMainInternal(addToHistory);
                break;
            case "login":
                loadLoginInternal(addToHistory);
                break;
            case "register":
                loadRegisterInternal(addToHistory);
                break;
            case "profile":
                loadProfileInternal(addToHistory);
                break;
            case "home":
                loadHomeInternal(addToHistory);
                break;
            case "class":
                // Can't reload class from history without class ID context
                loadHomeInternal(addToHistory);
                break;
            case "dashboard":
            case "admin":
                // Dashboard requires additional parameters, so we can't reload it from history
                // For now, just load main
                loadMainInternal(addToHistory);
                break;
        }
    }

    public static void loadMain() {
        loadMainInternal(true);
    }

    private static void loadMainInternal(boolean addToHistory) {
        try {
            if (addToHistory) {
                addToHistory("main");
            }
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/main.fxml"));
            Scene scene = new Scene(loader.load(), primaryStage.getWidth(), primaryStage.getHeight());
            primaryStage.setTitle("Learning Platform - Main");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Error loading main scene: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void loadLogin() {
        loadLoginInternal(true);
    }

    private static void loadLoginInternal(boolean addToHistory) {
        try {
            if (addToHistory) {
                addToHistory("login");
            }
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/login.fxml"));
            Scene scene = new Scene(loader.load(), primaryStage.getWidth(), primaryStage.getHeight());
            primaryStage.setTitle("Learning Platform - Login");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Error loading login scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadRegister() {
        loadRegisterInternal(true);
    }

    private static void loadRegisterInternal(boolean addToHistory) {
        try {
            if (addToHistory) {
                addToHistory("register");
            }
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/register.fxml"));
            Scene scene = new Scene(loader.load(), primaryStage.getWidth(), primaryStage.getHeight());
            primaryStage.setTitle("Learning Platform - Register");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Error loading register scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadDashboard(String userType, String token, String username) {
        try {
            String normalizedUserType = (userType == null || userType.isBlank()) ? "student" : userType;

            if ("admin".equals(normalizedUserType)) {
                addToHistory("admin");
                FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/admin.fxml"));
                Scene scene = new Scene(loader.load(), primaryStage.getWidth(), primaryStage.getHeight());
                AdminController controller = loader.getController();
                controller.setAuthToken(token);
                primaryStage.setTitle("Learning Platform - Admin Dashboard");
                primaryStage.setScene(scene);
                primaryStage.show();
            } else {
                addToHistory("dashboard");
                FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/dashboard.fxml"));
                Scene scene = new Scene(loader.load(), primaryStage.getWidth(), primaryStage.getHeight());
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

    public static void loadProfile() {
        loadProfileInternal(true);
    }

    private static void loadProfileInternal(boolean addToHistory) {
        try {
            if (addToHistory) {
                addToHistory("profile");
            }
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/profile.fxml"));
            Scene scene = new Scene(loader.load(), primaryStage.getWidth(), primaryStage.getHeight());
            primaryStage.setTitle("Learning Platform - Profile");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Error loading profile scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadHome() {
        loadHomeInternal(true);
    }

    private static void loadHomeInternal(boolean addToHistory) {
        try {
            if (addToHistory) {
                addToHistory("home");
            }
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/home.fxml"));
            Scene scene = new Scene(loader.load(), primaryStage.getWidth(), primaryStage.getHeight());
            primaryStage.setTitle("Learning Platform - Home");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Error loading home scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadClass(Integer classId) {
        try {
            addToHistory("class");
            ClassContextHolder.setClassId(classId);
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/class.fxml"));
            Scene scene = new Scene(loader.load(), primaryStage.getWidth(), primaryStage.getHeight());
            primaryStage.setTitle("Learning Platform - Class");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Error loading class scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
