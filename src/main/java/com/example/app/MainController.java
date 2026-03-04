// src/main/java/com/example/app/MainController.java
package com.example.app;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MainController {

    @FXML
    private void initialize() {
        System.out.println("MainController initialized");
    }

    @FXML
    private void onLoginButtonClick() {
        System.out.println("Login button clicked!");
        SceneManager.loadLogin();
    }

    @FXML
    private void onRegisterButtonClick() {
        System.out.println("Register button clicked!");
        SceneManager.loadRegister();
    }
}