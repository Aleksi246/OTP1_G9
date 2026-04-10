package com.example.app;

import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProfileController {

    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    private static final String API_URL = "http://localhost:7700";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        var unknown = LocaleManager.getString("profile.unknown");
        usernameLabel.setText(SessionManager.getUsername() != null ? SessionManager.getUsername() : unknown);
        emailLabel.setText(SessionManager.getEmail() != null ? SessionManager.getEmail() : unknown);
    }

    @FXML
    public void handleChangePassword() {
        String current = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (current.isEmpty()) { showMessage(LocaleManager.getString("profile.error.currentPasswordRequired"), true); return; }
        if (newPass.isEmpty()) { showMessage(LocaleManager.getString("profile.error.newPasswordRequired"), true); return; }
        if (confirm.isEmpty()) { showMessage(LocaleManager.getString("profile.error.confirmPasswordRequired"), true); return; }
        if (!newPass.equals(confirm)) { showMessage(LocaleManager.getString("profile.error.passwordMismatch"), true); return; }
        if (newPass.equals(current)) { showMessage(LocaleManager.getString("profile.error.passwordDifferent"), true); return; }
        if (newPass.length() < 6) { showMessage(LocaleManager.getString("profile.error.passwordMinLength"), true); return; }

        changePassword(current, newPass);
    }

    private void changePassword(String currentPassword, String newPassword) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("currentPassword", currentPassword);
            data.addProperty("newPassword", newPassword);

            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(API_URL + "/api/auth/change-password"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + SessionManager.getToken())
                            .PUT(HttpRequest.BodyPublishers.ofString(data.toString())).build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                showMessage(LocaleManager.getString("profile.success.passwordChanged"), false);
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
            } else if (response.statusCode() == 401) {
                showMessage(LocaleManager.getString("profile.error.currentPasswordIncorrect"), true);
            } else {
                showMessage(LocaleManager.getString("profile.error.passwordChangeFailed", response.statusCode()), true);
            }
        } catch (IOException | InterruptedException ex) {
            showMessage(LocaleManager.getString("profile.error.connection", ex.getMessage()), true);
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

    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setStyle(isError ? "-fx-text-fill: #d32f2f;" : "-fx-text-fill: #2e7d32;");
    }
}
