package com.example.app;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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

        String username = usernameField.getText();
        String email = emailField.getText();
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

        // --- Logic Success ---
        // Replace this with your database or API call logic
        System.out.println("Registering User: " + username + " with email: " + email);

        messageLabel.setStyle("-fx-text-fill: #27ae60;"); // Success color
        messageLabel.setText("Registration successful!");
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