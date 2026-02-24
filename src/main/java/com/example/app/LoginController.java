package com.example.app;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private TextField passwordField;
    @FXML private Label errorLabel;

    private static final String API_URL = "http://localhost:7700";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password required");
            return;
        }

        doLogin(username, password);
    }

    @FXML
    public void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password required");
            return;
        }

        doRegister(username, password);
    }

    private void doLogin(String username, String password) {
        try {
            String json = "{\"username\":\"" + escapeJson(username) + "\",\"password\":\"" + escapeJson(password) + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String token = extractToken(response.body());
                if (token != null) {
                    errorLabel.setText("Login successful!");
                    String userType = JWTHelper.getUserTypeFromToken(token);
                    SceneManager.loadDashboard(userType, token, username);
                } else {
                    showError("Failed to extract token from response");
                }
            } else {
                showError("Login failed: " + response.statusCode());
            }
        } catch (IOException | InterruptedException ex) {
            showError("Connection error: " + ex.getMessage());
        }
    }

    private void doRegister(String username, String password) {
        try {
            String json = "{\"username\":\"" + escapeJson(username) + "\",\"password\":\"" + escapeJson(password) + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                usernameField.setText("");
                passwordField.setText("");
                errorLabel.setText("Registration successful! Please log in.");
            } else if (response.statusCode() == 409) {
                showError("Username already exists");
            } else {
                showError("Registration failed: " + response.statusCode());
            }
        } catch (IOException | InterruptedException ex) {
            showError("Connection error: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private String extractToken(String response) {
        int start = response.indexOf("\"token\":\"");
        if (start == -1) return null;
        start += 9;
        int end = response.indexOf("\"", start);
        return end > start ? response.substring(start, end) : null;
    }

    private String escapeJson(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
