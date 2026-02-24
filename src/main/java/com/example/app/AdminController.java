package com.example.app;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AdminController {

    @FXML private TextField teacherUsernameField;
    @FXML private TextField teacherPasswordField;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private static final String API_URL = "http://localhost:7700";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String authToken;

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    @FXML
    public void handleCreateTeacher() {
        String username = teacherUsernameField.getText().trim();
        String password = teacherPasswordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password required");
            return;
        }

        try {
            String json = "{\"username\":\"" + escapeJson(username) + "\",\"password\":\"" + escapeJson(password) + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/api/admin/create-teacher"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + authToken)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                clearFields();
                showStatus("Teacher \"" + username + "\" created successfully!");
            } else if (response.statusCode() == 409) {
                showError("Username already exists");
            } else if (response.statusCode() == 403) {
                showError("Admin access required");
            } else {
                showError("Failed to create teacher: " + response.statusCode());
            }
        } catch (IOException | InterruptedException ex) {
            showError("Connection error: " + ex.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        SceneManager.loadLogin();
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void clearFields() {
        teacherUsernameField.setText("");
        teacherPasswordField.setText("");
    }

    private String escapeJson(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
