package com.example.app;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML private Label welcomeLabel;

    private String userType;
    private String username;

    public void setUserInfo(String userType, String username) {
        this.userType = userType;
        this.username = username;
        welcomeLabel.setText("Welcome, " + username + " (" + userType + ")");
    }

    @FXML
    public void handleLogout() {
        SceneManager.loadLogin();
    }
}
