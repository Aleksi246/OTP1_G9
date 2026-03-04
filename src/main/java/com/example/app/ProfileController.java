package com.example.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

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
            usernameLabel.setText("Unknown");
        }

        if (email != null) {
            emailLabel.setText(email);
        } else {
            emailLabel.setText("Unknown");
        }
    }

    @FXML
    public void handleChangePassword() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation
        if (currentPassword.isEmpty()) {
            showError("Current password is required");
            return;
        }

        if (newPassword.isEmpty()) {
            showError("New password is required");
            return;
        }

        if (confirmPassword.isEmpty()) {
            showError("Please confirm your new password");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("New passwords do not match");
            return;
        }

        if (newPassword.equals(currentPassword)) {
            showError("New password must be different from current password");
            return;
        }

        if (newPassword.length() < 6) {
            showError("New password must be at least 6 characters");
            return;
        }

        // Send password change request
        changePassword(currentPassword, newPassword);
    }

    private void changePassword(String currentPassword, String newPassword) {
        String json = "{\"currentPassword\":\"" + escapeJson(currentPassword) +
                     "\",\"newPassword\":\"" + escapeJson(newPassword) + "\"}";

        String token = SessionManager.getToken();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/api/users/change-password"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        showSuccess("Password changed successfully!");
                        currentPasswordField.clear();
                        newPasswordField.clear();
                        confirmPasswordField.clear();
                    } else if (response.statusCode() == 401) {
                        showError("Current password is incorrect");
                    } else {
                        showError("Password change failed: " + response.statusCode());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Connection error: " + ex.getMessage()));
                    return null;
                });
    }

    @FXML
    public void handleLogout() {
        SessionManager.clearSession();
        SceneManager.loadLogin();
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
