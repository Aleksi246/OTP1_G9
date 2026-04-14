package com.example.app;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Button toggleButton;

    private boolean isPasswordVisible = false;
    private static final String API_URL = "http://localhost:7700";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        passwordField.textProperty().bindBidirectional(passwordTextField.textProperty());
        messageLabel.setText("");
    }

    @FXML
    private void handleRegister() {
        messageLabel.setStyle("-fx-text-fill: #e74c3c;");

        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            messageLabel.setText("All fields are required!");
            return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            messageLabel.setText("Please enter a valid email address.");
            return;
        }
        if (!pass.equals(confirm)) {
            messageLabel.setText("Passwords do not match!");
            return;
        }
        if (pass.length() < 6) {
            messageLabel.setText("Password must be at least 6 characters.");
            return;
        }

        registerUser(username, email, pass);
    }

    private void registerUser(String username, String email, String password) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("username", username);
            data.addProperty("email", email);
            data.addProperty("password", password);

            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(API_URL + "/api/auth/register"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(data.toString())).build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                messageLabel.setStyle("-fx-text-fill: #27ae60;");
                messageLabel.setText("Registration successful! Logging in...");
                autoLogin(username, password);
            } else if (response.statusCode() == 409) {
                String body = response.body();
                if (body.contains("Username already exists")) messageLabel.setText("Username already taken");
                else if (body.contains("Email already exists")) messageLabel.setText("Email already taken");
                else messageLabel.setText("Username or email already taken");
            } else {
                messageLabel.setText("Registration failed: " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            messageLabel.setText("Registration interrupted");
        } catch (IOException e) {
            messageLabel.setText("Connection error: " + e.getMessage());
        }
    }

    private void autoLogin(String username, String password) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("username", username);
            data.addProperty("password", password);

            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(API_URL + "/api/auth/login"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(data.toString())).build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var json = JsonParser.parseString(response.body()).getAsJsonObject();
                String token = json.has("token") ? json.get("token").getAsString() : null;
                if (token != null) {
                    SessionManager.setSession(username, JWTHelper.getEmailFromToken(token), token, JWTHelper.getUserTypeFromToken(token));
                    SceneManager.loadHome();
                } else {
                    messageLabel.setText("Login failed: Unable to extract token");
                }
            } else {
                messageLabel.setText("Login failed: " + response.statusCode());
            }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          messageLabel.setText("Login interrupted");
        } catch (IOException e) {
            messageLabel.setText("Connection error: " + e.getMessage());
        }
    }

    @FXML
    private void togglePassword() {
        isPasswordVisible = !isPasswordVisible;
        passwordTextField.setVisible(isPasswordVisible);
        passwordTextField.setManaged(isPasswordVisible);
        passwordField.setVisible(!isPasswordVisible);
        passwordField.setManaged(!isPasswordVisible);
        toggleButton.setText(isPasswordVisible ? "Hide" : "Show");

        var target = isPasswordVisible ? passwordTextField : passwordField;
        target.requestFocus();
        target.selectEnd();
    }

    @FXML
    public void handleSwitchContext() {
        SceneManager.loadLogin();
    }
}