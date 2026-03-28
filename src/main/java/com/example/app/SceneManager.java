package com.example.app;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SceneManager {
    private static Stage primaryStage;
    private static List<String> navigationHistory = new ArrayList<>();
    private static int currentIndex = -1;
    private static String currentSceneName = "main";

    private static void applyScene(Scene scene, String title) {
        boolean wasFullScreen = primaryStage.isFullScreen();
        boolean wasMaximized = primaryStage.isMaximized();
        double previousX = primaryStage.getX();
        double previousY = primaryStage.getY();
        double previousWidth = primaryStage.getWidth();
        double previousHeight = primaryStage.getHeight();

        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.show();

        if (wasFullScreen) {
            // Full-screen mode can be dropped by setScene on some Linux window managers.
            Platform.runLater(() -> primaryStage.setFullScreen(true));
            return;
        }

        if (wasMaximized) {
            // Some Linux window managers drop maximized state on setScene.
            Platform.runLater(() -> primaryStage.setMaximized(true));
            return;
        }

        primaryStage.setX(previousX);
        primaryStage.setY(previousY);
        primaryStage.setWidth(previousWidth);
        primaryStage.setHeight(previousHeight);
    }

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
        }
    }

    public static void loadMain() {
        loadMainInternal(true);
    }

    private static void loadMainInternal(boolean addToHistory) {
        try {
            setCurrentScene("main");
            if (addToHistory) {
                addToHistory("main");
            }
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/main.fxml"));
            loader.setCharset(StandardCharsets.UTF_8);
            loader.setResources(LocaleManager.getBundle());
            Scene scene = new Scene(loader.load());
            scene.setNodeOrientation(LocaleManager.isRightToLeft() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
            applyScene(scene, LocaleManager.getBundle().getString("app.title") + " - Main");
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
            setCurrentScene("login");
            if (addToHistory) {
                addToHistory("login");
            }
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/login.fxml"));
            loader.setCharset(StandardCharsets.UTF_8);
            loader.setResources(LocaleManager.getBundle());
            Scene scene = new Scene(loader.load());
            scene.setNodeOrientation(LocaleManager.isRightToLeft() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
            applyScene(scene, LocaleManager.getBundle().getString("app.title") + " - Login");
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
            setCurrentScene("register");
            if (addToHistory) {
                addToHistory("register");
            }
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/register.fxml"));
            loader.setCharset(StandardCharsets.UTF_8);
            loader.setResources(LocaleManager.getBundle());
            Scene scene = new Scene(loader.load());
            scene.setNodeOrientation(LocaleManager.isRightToLeft() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
            applyScene(scene, LocaleManager.getBundle().getString("app.title") + " - Register");
        } catch (Exception e) {
            System.err.println("Error loading register scene: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void loadProfile() {
        loadProfileInternal(true);
    }

    private static void loadProfileInternal(boolean addToHistory) {
        try {
            setCurrentScene("profile");
            if (addToHistory) {
                addToHistory("profile");
            }
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/profile.fxml"));
            loader.setCharset(StandardCharsets.UTF_8);
            loader.setResources(LocaleManager.getBundle());
            Scene scene = new Scene(loader.load());
            scene.setNodeOrientation(LocaleManager.isRightToLeft() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
            applyScene(scene, LocaleManager.getBundle().getString("app.title") + " - Profile");
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
            setCurrentScene("home");
            if (addToHistory) {
                addToHistory("home");
            }
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/home.fxml"));
            loader.setCharset(StandardCharsets.UTF_8);
            loader.setResources(LocaleManager.getBundle());
            Scene scene = new Scene(loader.load());
            scene.setNodeOrientation(LocaleManager.isRightToLeft() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
            applyScene(scene, LocaleManager.getBundle().getString("app.title") + " - Home");
        } catch (Exception e) {
            System.err.println("Error loading home scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadClass(Integer classId) {
        try {
            setCurrentScene("class");
            addToHistory("class");
            ClassContextHolder.setClassId(classId);
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/app/class.fxml"));
            loader.setCharset(StandardCharsets.UTF_8);
            loader.setResources(LocaleManager.getBundle());
            Scene scene = new Scene(loader.load());
            scene.setNodeOrientation(LocaleManager.isRightToLeft() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
            applyScene(scene, LocaleManager.getBundle().getString("app.title") + " - Class");
        } catch (Exception e) {
            System.err.println("Error loading class scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void reloadCurrentScene() {
        switch (currentSceneName) {
            case "class":
                Integer classId = ClassContextHolder.getClassId();
                if (classId != null) {
                    loadClass(classId);
                    return;
                }
                break;
            default:
                loadSceneByName(currentSceneName, false);
                return;
        }
        loadHomeInternal(false);
    }

    private static void setCurrentScene(String sceneName) {
        currentSceneName = sceneName;
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
