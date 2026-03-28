// src/main/java/com/example/app/MainController.java
package com.example.app;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.text.TextAlignment;

public class MainController {

    @FXML
    private Label libraryDescLabel;

    @FXML
    private Label instructorsDescLabel;

    @FXML
    private Label reviewsDescLabel;

    @FXML
    private void initialize() {
        applyCardDescriptionLayout();
    }

    private void applyCardDescriptionLayout() {
        boolean rtl = LocaleManager.isRightToLeft();
        configureDescriptionLabel(libraryDescLabel, rtl);
        configureDescriptionLabel(instructorsDescLabel, rtl);
        configureDescriptionLabel(reviewsDescLabel, rtl);
    }

    private void configureDescriptionLabel(Label label, boolean rtl) {
        if (label == null) {
            return;
        }
        label.setWrapText(true);
        if (rtl) {
            label.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
            label.setAlignment(Pos.CENTER_RIGHT);
            label.setTextAlignment(TextAlignment.RIGHT);
        } else {
            label.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            label.setAlignment(Pos.CENTER);
            label.setTextAlignment(TextAlignment.CENTER);
        }
    }

    @FXML
    private void onLoginButtonClick() {
        SceneManager.loadLogin();
    }

    @FXML
    private void onRegisterButtonClick() {
        SceneManager.loadRegister();
    }
}