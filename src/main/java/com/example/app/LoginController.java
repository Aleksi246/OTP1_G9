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
    public void handlePrimaryAction() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText(LocaleManager.getString("login.error.required"));
            return;
        }

        doLogin(username, password);
    }

    @FXML
    public void handleSwitchContext() {
        SceneManager.loadRegister();
    }

    private void doLogin(String loginInput, String password) {
        try {
            boolean isEmailLogin = loginInput.contains("@");
            String json;
            if (isEmailLogin) {
                json = "{\"email\":\"" + escapeJson(loginInput) + "\",\"password\":\"" + escapeJson(password) + "\"}";
            } else {
                json = "{\"username\":\"" + escapeJson(loginInput) + "\",\"password\":\"" + escapeJson(password) + "\"}";
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                String token = extractJsonField(responseBody, "token");
                if (token != null) {
                    errorLabel.setText(LocaleManager.getString("login.success.login"));
                    String userType = JWTHelper.getUserTypeFromToken(token);
                    String email = extractJsonField(responseBody, "email");
                    if (email == null || email.isBlank()) {
                        email = JWTHelper.getEmailFromToken(token);
                    }
                    String username = extractJsonField(responseBody, "username");
                    if (username == null || username.isBlank()) {
                        username = loginInput;
                    }
                    SessionManager.setSession(username, email, token, userType);
                    SceneManager.loadHome();
                } else {
                    errorLabel.setText(LocaleManager.getString("login.error.token"));
                }
            } else {
                errorLabel.setText(LocaleManager.getString("login.error.failed", response.statusCode()));
            }
        } catch (IOException | InterruptedException ex) {
            errorLabel.setText(LocaleManager.getString("login.error.connection", ex.getMessage()));
        }
    }

    private String extractJsonField(String response, String fieldName) {
        String fieldPattern = "\"" + fieldName + "\":\"";
        int start = response.indexOf(fieldPattern);
        if (start == -1) return null;
        start += fieldPattern.length();
        int end = response.indexOf("\"", start);
        return end > start ? response.substring(start, end) : null;
    }

    private String escapeJson(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
