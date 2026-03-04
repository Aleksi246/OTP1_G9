package com.example.app;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML private Label welcomeLabel;

    public void setUserInfo(String userType, String username) {
        welcomeLabel.setText("Welcome, " + username + " (" + userType + ")");
    }

    @FXML
    public void handleLogout() {
        SceneManager.loadLogin();
    }
}
