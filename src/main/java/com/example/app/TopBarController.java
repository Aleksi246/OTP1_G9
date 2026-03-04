package com.example.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;

public class TopBarController {


    @FXML
    private ImageView learningPlatformImage;

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


    private void updateButtonStates() {
        boolean isLoggedIn = SessionManager.isLoggedIn();

        // Update learning platform image state
        if (learningPlatformImage != null) {
            learningPlatformImage.setDisable(!isLoggedIn);
            if (!isLoggedIn) {
                learningPlatformImage.setOpacity(0.3);
                learningPlatformImage.setStyle("-fx-cursor: default;");
            } else {
                learningPlatformImage.setOpacity(1.0);
                learningPlatformImage.setStyle("-fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 8, 0, 0, 3);");
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
    }

    // Method to allow external updates to button states
    public void refreshButtonStates() {
        updateButtonStates();
    }
}

