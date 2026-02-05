// src/main/java/com/example/app/MainController.java
package com.example.app;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MainController {
    @FXML
    private Label messageLabel;

    @FXML
    private Button clickButton;

    @FXML
    private void initialize() {
        messageLabel.setText("Ready");
    }

    @FXML
    private void onClick() {
        messageLabel.setText("Button clicked");
    }
}