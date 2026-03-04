package com.example.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
                    .uri(new URI("http://localhost:7700/api/participants/classes/" + userId))
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
        card.setPrefWidth(280);
        card.setMinHeight(200);
        card.setStyle(
                "-fx-background-color: white; " +
                "-fx-border-radius: 15; " +
                "-fx-background-radius: 15; " +
                "-fx-padding: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); " +
                "-fx-cursor: hand;"
        );
        card.setSpacing(15);

        // Class name label
        Label classNameLabel = new Label(className);
        classNameLabel.setStyle(
                "-fx-font-size: 18; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #2c3e50;"
        );
        classNameLabel.setWrapText(true);

        // Topic label
        Label topicLabel = new Label("Topic: " + topic);
        topicLabel.setStyle(
                "-fx-font-size: 12; " +
                "-fx-text-fill: #7f8c8d;"
        );
        topicLabel.setWrapText(true);

        // View button
        Button viewButton = new Button("View Class");
        viewButton.setStyle(
                "-fx-background-color: #3498db; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 10; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand;"
        );
        viewButton.setOnAction(e -> {
            // TODO: Navigate to class detail view
            System.out.println("Navigating to class: " + classId);
        });

        card.getChildren().addAll(classNameLabel, topicLabel, viewButton);
        card.setPadding(new Insets(20));

        // Add hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: #f8f9fa; " +
                    "-fx-border-radius: 15; " +
                    "-fx-background-radius: 15; " +
                    "-fx-padding: 20; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 15, 0, 0, 8); " +
                    "-fx-cursor: hand;"
            );
        });

        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: white; " +
                    "-fx-border-radius: 15; " +
                    "-fx-background-radius: 15; " +
                    "-fx-padding: 20; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); " +
                    "-fx-cursor: hand;"
            );
        });

        return card;
    }
}

