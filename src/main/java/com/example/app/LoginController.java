package com.example.app;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.text.MessageFormat;
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
    private boolean isRegisterMode = false;

    @FXML
    public void handlePrimaryAction() {
        if (isRegisterMode) {
            handleRegister();
        } else {
            handleLogin();
        }
    }

    @FXML
    public void handleSwitchContext() {
        SceneManager.loadRegister();
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError(LocaleManager.getBundle().getString("login.error.required"));
            return;
        }

        doLogin(username, password);
    }

    @FXML
    public void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError(LocaleManager.getBundle().getString("login.error.required"));
            return;
        }

        doRegister(username, password);
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
                String token = extractToken(responseBody);
                if (token != null) {
                    errorLabel.setText(LocaleManager.getBundle().getString("login.success.login"));
                    String userType = JWTHelper.getUserTypeFromToken(token);
                    String email = extractJsonField(responseBody, "email");
                    if (email == null || email.isBlank()) {
                        email = JWTHelper.getEmailFromToken(token);
                    }
                    String username = extractJsonField(responseBody, "username");
                    if (username == null || username.isBlank()) {
                        username = loginInput;
                    }
                    // Store session info
                    SessionManager.setSession(username, email, token, userType);
                    SceneManager.loadHome();
                } else {
                    showError(LocaleManager.getBundle().getString("login.error.token"));
                }
            } else {
                showError(MessageFormat.format(LocaleManager.getBundle().getString("login.error.failed"), response.statusCode()));
            }
        } catch (IOException | InterruptedException ex) {
            showError(MessageFormat.format(LocaleManager.getBundle().getString("login.error.connection"), ex.getMessage()));
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
                errorLabel.setText(LocaleManager.getBundle().getString("login.success.registration"));
            } else if (response.statusCode() == 409) {
                showError(LocaleManager.getBundle().getString("login.error.username.exists"));
            } else {
                showError(MessageFormat.format(LocaleManager.getBundle().getString("login.error.registration.failed"), response.statusCode()));
            }
        } catch (IOException | InterruptedException ex) {
            showError(MessageFormat.format(LocaleManager.getBundle().getString("login.error.connection"), ex.getMessage()));
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
