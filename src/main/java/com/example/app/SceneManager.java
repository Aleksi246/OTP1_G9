package com.example.app;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class SceneManager {
  private static Stage primaryStage;
  private static List<String> navigationHistory = new ArrayList<>();
  private static int currentIndex = -1;
  private static String currentSceneName = "main";
  private static final Logger logger = Logger.getLogger(SceneManager.class.getName());

  private SceneManager() {}

  public static void setPrimaryStage(Stage stage) {
    primaryStage = stage;
  }

  private static void loadScene(String name, boolean addToHistory) {
    try {
      var resource = SceneManager.class.getResource("/com/example/app/" + name + ".fxml");
      if (resource == null) {
        throw new IllegalStateException("Scene FXML not found: " + name);
      }

      FXMLLoader loader = new FXMLLoader(resource);
      loader.setCharset(StandardCharsets.UTF_8);
      loader.setResources(LocaleManager.getBundle());
      Scene scene = new Scene(loader.load());
      scene
              .setNodeOrientation(LocaleManager
                      .isRightToLeft() ? NodeOrientation
                                         .RIGHT_TO_LEFT : NodeOrientation
                                                          .LEFT_TO_RIGHT);

      // Commit navigation state only after successful scene construction.
      currentSceneName = name;
      if (addToHistory) {
        if (currentIndex < navigationHistory.size() - 1) {
          navigationHistory = new ArrayList<>(navigationHistory.subList(0, currentIndex + 1));
        }
        navigationHistory.add(name);
        currentIndex++;
      }

      applyScene(scene, LocaleManager.getString("app.title") + " - " + name);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e, () -> "Error loading " + name + " scene");
    }
  }

  private static void applyScene(Scene scene, String title) {
    boolean wasFullScreen = primaryStage.isFullScreen();
    boolean wasMaximized = primaryStage.isMaximized();
    double prevX = primaryStage.getX();
    double prevY = primaryStage.getY();
    double prevW = primaryStage.getWidth();
    double prevH = primaryStage.getHeight();

    primaryStage.setTitle(title);
    primaryStage.setScene(scene);
    primaryStage.show();

    if (wasFullScreen) {
      Platform.runLater(() -> primaryStage.setFullScreen(true));
    } else if (wasMaximized) {
      Platform.runLater(() -> primaryStage.setMaximized(true));
    } else {
      primaryStage.setX(prevX);
      primaryStage.setY(prevY);
      primaryStage.setWidth(prevW);
      primaryStage.setHeight(prevH);
    }
  }

  public static void loadMain() {
    loadScene("main", true);
  }

  public static void loadLogin() {
    loadScene("login", true);
  }

  public static void loadRegister() {
    loadScene("register", true);
  }

  public static void loadProfile()  {
    loadScene("profile", true);
  }

  public static void loadHome()     {
    loadScene("home", true);
  }

  public static void loadClass(Integer classId) {
    ClassContextHolder.setClassId(classId);
    loadScene("class", true);
  }

  public static void goBack() {
    if (currentIndex > 0) {
      currentIndex--;
      loadScene(navigationHistory.get(currentIndex), false);
    }
  }

  public static void goForward() {
    if (currentIndex < navigationHistory.size() - 1) {
      currentIndex++;
      loadScene(navigationHistory.get(currentIndex), false);
    }
  }

  public static void reloadCurrentScene() {
    if ("class".equals(currentSceneName)) {
      Integer classId = ClassContextHolder.getClassId();
      if (classId != null) {
        loadClass(classId);
        return;
      }
    }
    loadScene(currentSceneName, false);
  }
}
