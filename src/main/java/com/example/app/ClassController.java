package com.example.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class ClassController {

    @FXML
    private Label classNameLabel;

    @FXML
    private Label classIdLabel;

    @FXML
    private Label topicLabel;

    @FXML
    private Label creatorLabel;

    @FXML
    private VBox contentContainer;

    @FXML
    private Button joinButton;

    private Integer classId;
    private Integer userId;
    private String token;

    @FXML
    private void initialize() {
        if (!SessionManager.isLoggedIn()) {
            Platform.runLater(() -> SceneManager.loadLogin());
            return;
        }

        token = SessionManager.getToken();
        userId = SessionManager.getUserId();

        // Retrieve classId from SceneManager or parameter
        classId = getClassIdFromContext();

        if (classId != null) {
            loadClassDetails();
        } else {
            showError("Error", "Class ID not provided");
        }
    }

    private Integer getClassIdFromContext() {
        // This will be set via a method before the scene is shown
        // For now, try to get it from a static holder or return null
        Integer id = ClassContextHolder.getClassId();
        ClassContextHolder.clear();
        return id;
    }

    private void loadClassDetails() {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:7700/api/courses/" + classId))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject courseJson = JsonParser.parseString(response.body()).getAsJsonObject();
                    String className = courseJson.get("className").getAsString();
                    String topic = courseJson.has("topic") ? courseJson.get("topic").getAsString() : "No topic";
                    Integer creatorId = courseJson.get("creatorId").getAsInt();

                    Platform.runLater(() -> {
                        classNameLabel.setText(className);
                        classIdLabel.setText("Class ID: " + classId);
                        topicLabel.setText("Topic: " + topic);
                        creatorLabel.setText("Created by: User #" + creatorId);

                        // Check if current user is already enrolled
                        checkEnrollmentStatus();
                    });
                } else {
                    Platform.runLater(() -> showError("Error", "Failed to load class details"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Error", "Failed to load class: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    private void checkEnrollmentStatus() {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:7700/api/participants/user/" + userId))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    var classesArray = JsonParser.parseString(response.body()).getAsJsonArray();
                    boolean isEnrolled = false;
                    for (var element : classesArray) {
                        if (element.getAsInt() == classId) {
                            isEnrolled = true;
                            break;
                        }
                    }

                    boolean finalIsEnrolled = isEnrolled;
                    Platform.runLater(() -> {
                        if (finalIsEnrolled) {
                            joinButton.setText("Already Enrolled");
                            joinButton.setDisable(true);
                        } else {
                            joinButton.setText("Join Class");
                            joinButton.setDisable(false);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleJoinClass() {
        new Thread(() -> {
            try {
                JsonObject enrollData = new JsonObject();
                enrollData.addProperty("userId", userId);
                enrollData.addProperty("classId", classId);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:7700/api/participants/enroll"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(enrollData.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        showSuccess("Success", "You have successfully joined the class!");
                        joinButton.setText("Already Enrolled");
                        joinButton.setDisable(true);
                    } else if (response.statusCode() == 403) {
                        showError("Error", "Only the class creator can enroll users");
                    } else {
                        showError("Error", "Failed to join class: " + response.statusCode());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Error", "Failed to join class: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    @FXML
    private void handleGoBack() {
        SceneManager.goBack();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

// Helper class to pass context between scenes
class ClassContextHolder {
    private static Integer classId;

    public static void setClassId(Integer id) {
        classId = id;
    }

    public static Integer getClassId() {
        return classId;
    }

    public static void clear() {
        classId = null;
    }
}

