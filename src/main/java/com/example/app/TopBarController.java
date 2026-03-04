package com.example.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class TopBarController {

    @FXML
    private Button backButton;

    @FXML
    private Button forwardButton;

    @FXML
    private Button learningPlatformButton;

    @FXML
    private Button profileButton;

    @FXML
    private void initialize() {
        // Update button states after a short delay to allow scene to fully load
        Platform.runLater(this::updateButtonStates);
    }

    @FXML
    public void handleLearningPlatformClick() {
        // Only navigate if user is logged in
        if (SessionManager.isLoggedIn()) {
            SceneManager.loadHome();
            // Update button states after navigation
            Platform.runLater(this::updateButtonStates);
        }
        // Do nothing if not logged in (button should be disabled)
    }

    @FXML
    public void handleSwitchToProfile() {
        if (SessionManager.isLoggedIn()) {
            SceneManager.loadProfile();
        } else {
            // Navigate to login if not logged in
            SceneManager.loadLogin();
        }
        // Update button states after navigation
        Platform.runLater(this::updateButtonStates);
    }

    @FXML
    private void handleBack() {
        SceneManager.goBack();
        // Update button states after navigation
        Platform.runLater(this::updateButtonStates);
    }

    @FXML
    private void handleForward() {
        SceneManager.goForward();
        // Update button states after navigation
        Platform.runLater(this::updateButtonStates);
    }

    private void updateButtonStates() {
        boolean isLoggedIn = SessionManager.isLoggedIn();

        // Update learning platform button state
        if (learningPlatformButton != null) {
            learningPlatformButton.setDisable(!isLoggedIn);
            if (!isLoggedIn) {
                learningPlatformButton.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #999999; -fx-cursor: default;");
            } else {
                learningPlatformButton.setStyle("");
            }
        }

        // Update profile button text and styling
        if (profileButton != null) {
            if (isLoggedIn) {
                profileButton.setText("Profile");
                profileButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;");
            } else {
                profileButton.setText("Log in");
                profileButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;");
            }
        }

        if (backButton != null) {
            backButton.setDisable(!SceneManager.canGoBack());
            if (!SceneManager.canGoBack()) {
                backButton.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #999999; -fx-cursor: default; -fx-padding: 8 12 8 12; -fx-background-radius: 5; -fx-border-radius: 5; -fx-font-size: 16px; -fx-font-weight: bold;");
            } else {
                backButton.setStyle("-fx-background-color: #f0f0f0; -fx-cursor: hand; -fx-padding: 8 12 8 12; -fx-background-radius: 5; -fx-border-radius: 5; -fx-font-size: 16px; -fx-font-weight: bold;");
            }
        }
        if (forwardButton != null) {
            forwardButton.setDisable(!SceneManager.canGoForward());
            if (!SceneManager.canGoForward()) {
                forwardButton.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #999999; -fx-cursor: default; -fx-padding: 8 12 8 12; -fx-background-radius: 5; -fx-border-radius: 5; -fx-font-size: 16px; -fx-font-weight: bold;");
            } else {
                forwardButton.setStyle("-fx-background-color: #f0f0f0; -fx-cursor: hand; -fx-padding: 8 12 8 12; -fx-background-radius: 5; -fx-border-radius: 5; -fx-font-size: 16px; -fx-font-weight: bold;");
            }
        }
    }

    // Method to allow external updates to button states
    public void refreshButtonStates() {
        updateButtonStates();
    }
}

