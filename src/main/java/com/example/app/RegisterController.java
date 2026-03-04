package com.example.app;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordTextField; // Plain text version for "Show Password"

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button toggleButton;

    private boolean isPasswordVisible = false;
    private static final String API_URL = "http://localhost:7700";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        // Sync the masked and plain text fields so they always have the same text
        passwordField.textProperty().bindBidirectional(passwordTextField.textProperty());

        // Ensure error label is clear on startup
        messageLabel.setText("");
    }

    @FXML
    private void handleRegister() {
        messageLabel.setStyle("-fx-text-fill: #e74c3c;"); // Default to error color

        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        // 1. Basic Validation
        if (username.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            messageLabel.setText("All fields are required!");
            return;
        }

        // 2. Email Validation (Simple check)
        if (!email.contains("@") || !email.contains(".")) {
            messageLabel.setText("Please enter a valid email address.");
            return;
        }

        // 3. Password Match Validation
        if (!pass.equals(confirm)) {
            messageLabel.setText("Passwords do not match!");
            return;
        }

        // 4. Password Strength (Optional check)
        if (pass.length() < 6) {
            messageLabel.setText("Password must be at least 6 characters.");
            return;
        }

        // 5. Make API call to register
        registerUser(username, email, pass);
    }

    private void registerUser(String username, String email, String password) {
        try {
            String json = "{\"username\":\"" + escapeJson(username) +
                         "\",\"email\":\"" + escapeJson(email) +
                         "\",\"password\":\"" + escapeJson(password) + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                // Registration successful, now auto-login
                messageLabel.setStyle("-fx-text-fill: #27ae60;");
                messageLabel.setText("Registration successful! Logging in...");
                autoLogin(username, password);
            } else if (response.statusCode() == 409) {
                // Conflict - check which field is already taken
                String responseBody = response.body();
                if (responseBody.contains("Username already exists")) {
                    messageLabel.setText("Username already taken");
                } else if (responseBody.contains("Email already exists")) {
                    messageLabel.setText("Email already taken");
                } else {
                    messageLabel.setText("Username or email already taken");
                }
            } else {
                messageLabel.setText("Registration failed: " + response.statusCode());
            }
        } catch (IOException | InterruptedException ex) {
            messageLabel.setText("Connection error: " + ex.getMessage());
        }
    }

    private void autoLogin(String username, String password) {
        try {
            String json = "{\"username\":\"" + escapeJson(username) +
                         "\",\"password\":\"" + escapeJson(password) + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String token = extractToken(response.body());
                if (token != null) {
                    String userType = JWTHelper.getUserTypeFromToken(token);
                    String email = JWTHelper.getEmailFromToken(token);
                    // Store session info
                    SessionManager.setSession(username, email, token, userType);
                    SceneManager.loadDashboard(userType, token, username);
                } else {
                    messageLabel.setText("Login failed: Unable to extract token");
                }
            } else {
                messageLabel.setText("Login failed: " + response.statusCode());
            }
        } catch (IOException | InterruptedException ex) {
            messageLabel.setText("Connection error: " + ex.getMessage());
        }
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

    @FXML
    private void togglePassword() {
        if (!isPasswordVisible) {
            // Show Plain Text
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);

            toggleButton.setText("🙈");
            isPasswordVisible = true;

            // Transfer focus to the visible field so the cursor doesn't disappear
            passwordTextField.requestFocus();
            passwordTextField.selectEnd();
        } else {
            // Show Masked
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);

            toggleButton.setText("👁");
            isPasswordVisible = false;

            passwordField.requestFocus();
            passwordField.selectEnd();
        }
    }

    @FXML
    public void handleSwitchContext() {
        SceneManager.loadLogin();
    }
}