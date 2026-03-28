package com.example.app;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

import java.io.IOException;
import java.text.MessageFormat;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProfileController {

    @FXML
    private Label usernameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label messageLabel;

    private static final String API_URL = "http://localhost:7700";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        loadUserProfile();
    }

    private void loadUserProfile() {
        String username = SessionManager.getUsername();
        String email = SessionManager.getEmail();

        if (username != null) {
            usernameLabel.setText(username);
        } else {
            usernameLabel.setText(LocaleManager.getBundle().getString("profile.unknown"));
        }

        if (email != null) {
            emailLabel.setText(email);
        } else {
            emailLabel.setText(LocaleManager.getBundle().getString("profile.unknown"));
        }
    }

    @FXML
    public void handleChangePassword() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation
        if (currentPassword.isEmpty()) {
            showError(LocaleManager.getBundle().getString("profile.error.currentPasswordRequired"));
            return;
        }

        if (newPassword.isEmpty()) {
            showError(LocaleManager.getBundle().getString("profile.error.newPasswordRequired"));
            return;
        }

        if (confirmPassword.isEmpty()) {
            showError(LocaleManager.getBundle().getString("profile.error.confirmPasswordRequired"));
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError(LocaleManager.getBundle().getString("profile.error.passwordMismatch"));
            return;
        }

        if (newPassword.equals(currentPassword)) {
            showError(LocaleManager.getBundle().getString("profile.error.passwordDifferent"));
            return;
        }

        if (newPassword.length() < 6) {
            showError(LocaleManager.getBundle().getString("profile.error.passwordMinLength"));
            return;
        }

        // Send password change request
        changePassword(currentPassword, newPassword);
    }

    private void changePassword(String currentPassword, String newPassword) {
        try {
            String json = "{\"currentPassword\":\"" + escapeJson(currentPassword) +
                         "\",\"newPassword\":\"" + escapeJson(newPassword) + "\"}";

            String token = SessionManager.getToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/api/auth/change-password"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                showSuccess(LocaleManager.getBundle().getString("profile.success.passwordChanged"));
                // Clear the password fields
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
            } else if (response.statusCode() == 401) {
                showError(LocaleManager.getBundle().getString("profile.error.currentPasswordIncorrect"));
            } else {
                showError(MessageFormat.format(LocaleManager.getBundle().getString("profile.error.passwordChangeFailed"), response.statusCode()));
            }
        } catch (IOException | InterruptedException ex) {
            showError(MessageFormat.format(LocaleManager.getBundle().getString("profile.error.connection"), ex.getMessage()));
        }
    }

    @FXML
    public void handleLogout() {
        SessionManager.clearSession();
        SceneManager.loadLogin();
    }

    @FXML
    public void handleBackToHome() {
        SceneManager.loadHome();
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #d32f2f;");
    }

    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #2e7d32;");
    }

    private String escapeJson(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
