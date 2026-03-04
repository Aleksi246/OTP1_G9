package com.example.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class HomeController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private FlowPane classesContainer;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Label loadingLabel;

    @FXML
    private void initialize() {
        // Check if user is logged in
        if (!SessionManager.isLoggedIn()) {
            // Redirect to login if not logged in
            Platform.runLater(() -> SceneManager.loadLogin());
            return;
        }

        // Set welcome message
        String username = SessionManager.getUsername();
        welcomeLabel.setText("Welcome, " + username + "! 📚");

        // Load user's classes
        loadUserClasses();
    }

    private void loadUserClasses() {
        new Thread(() -> {
            try {
                String token = SessionManager.getToken();
                String email = SessionManager.getEmail();

                if (token == null || email == null) {
                    Platform.runLater(() -> {
                        loadingLabel.setText("Error: Not authenticated");
                    });
                    return;
                }

                // Fetch user ID first
                Integer userId = fetchUserId(email, token);
                if (userId == null) {
                    Platform.runLater(() -> {
                        loadingLabel.setText("Error: Could not fetch user information");
                    });
                    return;
                }

                // Store userId in SessionManager for future use
                SessionManager.setUserId(userId);

                // Fetch classes for this user
                List<Integer> classIds = fetchUserClasses(userId, token);

                Platform.runLater(() -> {
                    if (classIds.isEmpty()) {
                        loadingLabel.setText("No classes yet. Ask your instructor to add you to a class!");
                        classesContainer.getChildren().clear();
                    } else {
                        loadingLabel.setText("");
                        displayClasses(classIds, token);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingLabel.setText("Error loading classes: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    private Integer fetchUserId(String email, String token) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:7700/api/users/by-email/" + email))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonElement element = JsonParser.parseString(response.body());
                if (element.isJsonObject()) {
                    return element.getAsJsonObject().get("userId").getAsInt();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Integer> fetchUserClasses(Integer userId, String token) {
        List<Integer> classIds = new java.util.ArrayList<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:7700/api/participants/user/" + userId))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                for (JsonElement element : array) {
                    classIds.add(element.getAsInt());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classIds;
    }

    private void displayClasses(List<Integer> classIds, String token) {
        classesContainer.getChildren().clear();

        for (Integer classId : classIds) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:7700/api/courses/" + classId))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    var courseJson = JsonParser.parseString(response.body()).getAsJsonObject();
                    String className = courseJson.get("className").getAsString();
                    String topic = courseJson.has("topic") ? courseJson.get("topic").getAsString() : "No topic";

                    Platform.runLater(() -> {
                        VBox classCard = createClassCard(className, topic, classId);
                        classesContainer.getChildren().add(classCard);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private VBox createClassCard(String className, String topic, Integer classId) {
        VBox card = new VBox();
        card.setPrefWidth(320);
        card.setMinHeight(280);
        card.setMaxHeight(380);
        card.setSpacing(0);

        // Generate a unique color for each class based on its ID
        String[] colors = {"#5B6FCC", "#E85554", "#43A1A9", "#F19A38", "#7D5FA3", "#4A90C5", "#2ECC71", "#E67E22"};
        String headerColor = colors[classId % colors.length];

        // Create header with color
        VBox headerBox = new VBox();
        headerBox.setPrefHeight(100);
        headerBox.setStyle(
                "-fx-background-color: " + headerColor + "; " +
                "-fx-background-radius: 14 14 0 0; " +
                "-fx-padding: 20;"
        );
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setSpacing(8);

        // Class name in header
        Label classNameLabel = new Label(className);
        classNameLabel.setStyle(
                "-fx-font-size: 18; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: white;"
        );
        classNameLabel.setWrapText(true);
        classNameLabel.setMaxWidth(280);

        headerBox.getChildren().add(classNameLabel);

        // Content box with padding
        VBox contentBox = new VBox();
        contentBox.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 0 0 14 14; " +
                "-fx-padding: 20;"
        );
        contentBox.setSpacing(12);
        contentBox.setPrefHeight(180);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        // Topic label with icon
        Label topicLabelHeader = new Label("📚 Topic");
        topicLabelHeader.setStyle(
                "-fx-font-size: 12; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #5a6c7d;"
        );

        Label topicLabel = new Label(topic == null || topic.isEmpty() ? "No topic set" : topic);
        topicLabel.setStyle(
                "-fx-font-size: 14; " +
                "-fx-text-fill: #2c3e50; " +
                "-fx-wrap-text: true;"
        );
        topicLabel.setWrapText(true);
        topicLabel.setMaxWidth(280);

        // Class ID label (smaller, subtle)
        Label classIdLabel = new Label("Class ID: " + classId);
        classIdLabel.setStyle(
                "-fx-font-size: 11; " +
                "-fx-text-fill: #a5b3bf; " +
                "-fx-font-family: 'Courier New';"
        );

        // Spacer to push button down
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // View button
        Button viewButton = new Button("View Class");
        viewButton.setPrefWidth(280);
        viewButton.setPrefHeight(42);
        viewButton.setStyle(
                "-fx-background-color: " + headerColor + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 13; " +
                "-fx-padding: 0; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-border: none;"
        );
        viewButton.setOnAction(e -> {
            SceneManager.loadClass(classId);
        });

        contentBox.getChildren().addAll(topicLabelHeader, topicLabel, classIdLabel, spacer, viewButton);

        card.getChildren().addAll(headerBox, contentBox);
        card.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 14; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 12, 0, 0, 4); " +
                "-fx-cursor: hand;"
        );

        // Add hover effect with smooth transition
        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: white; " +
                    "-fx-background-radius: 14; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 20, 0, 0, 10); " +
                    "-fx-cursor: hand;"
            );
        });

        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: white; " +
                    "-fx-background-radius: 14; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 12, 0, 0, 4); " +
                    "-fx-cursor: hand;"
            );
        });

        return card;
    }

    @FXML
    private void handleJoinClass() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Join Class");
        dialog.setHeaderText("Enter Class ID to join");

        TextField classIdField = new TextField();
        classIdField.setPromptText("Enter class ID");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().add(classIdField);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.OK,
                javafx.scene.control.ButtonType.CANCEL
        );

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !classIdField.getText().isEmpty()) {
            try {
                Integer classId = Integer.parseInt(classIdField.getText());
                // Navigate to class view
                SceneManager.loadClass(classId);
            } catch (NumberFormatException e) {
                showError("Invalid Input", "Please enter a valid class ID number");
            }
        }
    }

    @FXML
    private void handleCreateClass() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Create Class");
        dialog.setHeaderText("Create a new class");

        TextField classNameField = new TextField();
        classNameField.setPromptText("Enter class name");

        TextField topicField = new TextField();
        topicField.setPromptText("Enter topic (optional)");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(
                new Label("Class Name:"),
                classNameField,
                new Label("Topic:"),
                topicField
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.OK,
                javafx.scene.control.ButtonType.CANCEL
        );

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !classNameField.getText().isEmpty()) {
            createClassOnServer(classNameField.getText(), topicField.getText());
        }
    }

    private void createClassOnServer(String className, String topic) {
        new Thread(() -> {
            try {
                String token = SessionManager.getToken();

                JsonObject courseData = new JsonObject();
                courseData.addProperty("className", className);
                courseData.addProperty("topic", topic);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:7700/api/courses"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(courseData.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        showSuccess("Success", "Class created successfully!");
                        // Reload classes
                        loadUserClasses();
                    } else {
                        showError("Error", "Failed to create class: " + response.statusCode());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Error", "Failed to create class: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
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

