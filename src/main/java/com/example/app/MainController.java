package com.example.app;

import javafx.fxml.FXML;

public class MainController {

    @FXML
    private void onLoginButtonClick() {
        SceneManager.loadLogin();
    }

    @FXML
    private void onRegisterButtonClick() {
        SceneManager.loadRegister();
    }
}