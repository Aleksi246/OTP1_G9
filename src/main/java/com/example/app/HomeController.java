package com.example.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeController {

    @FXML private Label welcomeLabel;
    @FXML private Label subtitleLabel;
    @FXML private MenuButton manageClassesMenuButton;
    @FXML private MenuItem joinClassMenuItem;
    @FXML private MenuItem createClassMenuItem;
    @FXML private FlowPane classesContainer;
    @FXML private Label loadingLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final Logger logger = Logger.getLogger(HomeController.class.getName());

    @FXML
    private void initialize() {
        applyStaticTranslations();
        if (!SessionManager.isLoggedIn()) {
            Platform.runLater(SceneManager::loadLogin);
            return;
        }
        welcomeLabel.setText(LocaleManager.getString("home.welcome", SessionManager.getUsername()));
        loadUserClasses();
    }

    private void applyStaticTranslations() {
        if (welcomeLabel != null) {
            welcomeLabel.setText(LocaleManager.getString("home.welcome", SessionManager.getUsername()));
        }
        if (subtitleLabel != null) {
            subtitleLabel.setText(LocaleManager.getString("home.subtitle"));
        }
        if (manageClassesMenuButton != null) {
            manageClassesMenuButton.setText(LocaleManager.getString("home.manageClasses"));
        }
        if (joinClassMenuItem != null) {
            joinClassMenuItem.setText(LocaleManager.getString("home.joinClass"));
        }
        if (createClassMenuItem != null) {
            createClassMenuItem.setText(LocaleManager.getString("home.createClass"));
        }
        if (loadingLabel != null) {
            loadingLabel.setText(LocaleManager.getString("home.loadingClasses"));
        }
    }

    private void loadUserClasses() {
        Platform.runLater(() -> {
            loadingLabel.setText(LocaleManager.getString("home.loadingClasses"));
            classesContainer.getChildren().clear();
        });
        runAsync(() -> {
            try {
                String token = SessionManager.getToken();
                String email = SessionManager.getEmail();
                if (token == null || email == null) {
                    Platform.runLater(() -> loadingLabel.setText(LocaleManager.getString("home.error.notAuthenticated")));
                    return;
                }

                Integer userId = fetchUserId(email, token);
                if (userId == null) {
                    Platform.runLater(() -> loadingLabel.setText(LocaleManager.getString("home.error.fetchUser")));
                    return;
                }
                SessionManager.setUserId(userId);

                List<Integer> classIds = fetchUserClasses(userId, token);
                Platform.runLater(() -> {
                    if (classIds.isEmpty()) {
                        loadingLabel.setText(LocaleManager.getString("home.noClasses"));
                        classesContainer.getChildren().clear();
                    } else {
                        loadingLabel.setText("");
                        displayClasses(classIds, token);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> loadingLabel.setText(
                        LocaleManager.getString("home.error.loadingClasses", e.getMessage())));
            }
        });
    }

    private Integer fetchUserId(String email, String token) {
        try {
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(new URI("http://localhost:7700/api/users/by-email/" + encodedEmail))
                            .header("Authorization", "Bearer " + token).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return JsonParser.parseString(response.body()).getAsJsonObject().get("userId").getAsInt();
            }
        } catch (Exception e) { logger.log(Level.SEVERE, "Error fetching user ID", e); }
        return null;
    }

    private List<Integer> fetchUserClasses(Integer userId, String token) {
        List<Integer> classIds = new ArrayList<>();
        try {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(new URI("http://localhost:7700/api/participants/user/" + userId))
                            .header("Authorization", "Bearer " + token).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                for (JsonElement el : JsonParser.parseString(response.body()).getAsJsonArray()) {
                    classIds.add(el.getAsInt());
                }
            }
        } catch (Exception e) { logger.log(Level.SEVERE, "Error fetching user classes", e); }
        return classIds;
    }

    private void displayClasses(List<Integer> classIds, String token) {
        classesContainer.getChildren().clear();
        for (Integer classId : classIds) {
            runAsync(() -> fetchAndRenderClassCard(classId, token));
        }
    }

    private void fetchAndRenderClassCard(Integer classId, String token) {
        try {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(new URI("http://localhost:7700/api/courses/" + classId))
                            .header("Authorization", "Bearer " + token).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return;
            }

            JsonObject course = JsonParser.parseString(response.body()).getAsJsonObject();
            String name = course.has("className") && !course.get("className").isJsonNull()
                    ? course.get("className").getAsString()
                    : "Class " + classId;
            String topic = LocaleManager.getString("home.noTopic");
            if (course.has("topic") && !course.get("topic").isJsonNull()) {
                String rawTopic = course.get("topic").getAsString();
                if (rawTopic != null && !rawTopic.isBlank()) {
                    topic = rawTopic;
                }
            }

            final String finalName = name;
            final String finalTopic = topic;
            Platform.runLater(() -> classesContainer.getChildren().add(createClassCard(finalName, finalTopic, classId)));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching class card", e);
        }
    }

    private VBox createClassCard(String className, String topic, Integer classId) {
        String[] colors = {"#3b82f6", "#8b5cf6", "#06b6d4", "#f59e0b", "#10b981", "#ef4444", "#6366f1", "#f97316"};
        String accent = colors[Math.abs(classId % colors.length)];

        VBox headerBox = new VBox();
        headerBox.setPrefHeight(90);
        headerBox.setAlignment(Pos.BOTTOM_LEFT);
        headerBox.setSpacing(4);
        headerBox.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 14 14 0 0; -fx-padding: 16 18 14 18;");

        Label nameLabel = new Label(className);
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: 'Segoe UI';");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(264);

        Label idBadge = new Label("ID " + classId);
        idBadge.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.75); -fx-font-family: 'Segoe UI'; -fx-font-weight: bold;");
        headerBox.getChildren().addAll(nameLabel, idBadge);

        Label topicLabel = new Label(topic == null || topic.isEmpty() ? LocaleManager.getString("home.noTopic") : topic);
        topicLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569; -fx-font-family: 'Segoe UI';");
        topicLabel.setWrapText(true);
        topicLabel.setMaxWidth(264);

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button viewButton = new Button(LocaleManager.getString("home.openClass"));
        viewButton.setPrefWidth(264);
        viewButton.setPrefHeight(38);
        viewButton.setStyle("-fx-background-color: " + accent + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Segoe UI';");
        viewButton.setOnAction(e -> SceneManager.loadClass(classId));

        VBox contentBox = new VBox(10, topicLabel, spacer, viewButton);
        contentBox.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 14 14; -fx-padding: 16 18 18 18;");
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        String cardStyle = "-fx-background-color: white; -fx-background-radius: 14; -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.09), 16, 0, 0, 4); -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: white; -fx-background-radius: 14; -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.16), 22, 0, 0, 8); -fx-cursor: hand;";

        VBox card = new VBox(0, headerBox, contentBox);
        card.setPrefWidth(300);
        card.setMinHeight(240);
        card.setMaxHeight(360);
        card.setStyle(cardStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(cardStyle));
        card.setOnMouseClicked(e -> SceneManager.loadClass(classId));
        return card;
    }

    @FXML
    private void handleJoinClass() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(LocaleManager.getString("home.joinClass.title"));
        dialog.setHeaderText(LocaleManager.getString("home.joinClass.header"));

        TextField classIdField = new TextField();
        classIdField.setPromptText(LocaleManager.getString("home.joinClass.prompt"));

        VBox content = new VBox(10, classIdField);
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait();
        if (!classIdField.getText().isEmpty()) {
            try {
                SceneManager.loadClass(Integer.parseInt(classIdField.getText()));
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, LocaleManager.getBundle().getString("home.error.title"),
                        LocaleManager.getBundle().getString("home.invalidInput"));
            }
        }
    }

    @FXML
    private void handleCreateClass() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(LocaleManager.getString("home.createClass.title"));
        dialog.setHeaderText(LocaleManager.getString("home.createClass.header"));

        TextField classNameField = new TextField();
        classNameField.setPromptText(LocaleManager.getString("home.createClass.namePrompt"));
        TextField topicField = new TextField();
        topicField.setPromptText(LocaleManager.getString("home.createClass.topicPrompt"));

        VBox content = new VBox(10,
                new Label(LocaleManager.getString("home.createClass.nameLabel")), classNameField,
                new Label(LocaleManager.getString("home.createClass.topicLabel")), topicField);
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait();
        if (!classNameField.getText().isEmpty()) {
            createClassOnServer(classNameField.getText(), topicField.getText());
        }
    }

    private void createClassOnServer(String className, String topic) {
        runAsync(() -> {
            try {
                JsonObject data = new JsonObject();
                data.addProperty("className", className);
                data.addProperty("topic", topic);

                HttpResponse<String> response = httpClient.send(
                        HttpRequest.newBuilder()
                                .uri(new URI("http://localhost:7700/api/courses"))
                                .header("Authorization", "Bearer " + SessionManager.getToken())
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(data.toString())).build(),
                        HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        showAlert(Alert.AlertType.INFORMATION, LocaleManager.getString("home.success.title"),
                                LocaleManager.getString("home.createClass.success"));
                        loadUserClasses();
                    } else {
                        showAlert(Alert.AlertType.ERROR, LocaleManager.getString("home.error.title"),
                                LocaleManager.getString("home.error.createClass", response.statusCode()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, LocaleManager.getString("home.error.title"),
                        LocaleManager.getString("home.error.createClassException", e.getMessage())));
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void runAsync(Runnable task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
