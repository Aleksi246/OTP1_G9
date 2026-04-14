package com.example.app;

import com.example.service.AuthService;
import com.example.service.LoginResult;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.http.HttpClient;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private TextField passwordField;
    @FXML private Label errorLabel;

    private static final String API_URL = "http://localhost:7700";

    private final AuthService authService =
            new AuthService(HttpClient.newHttpClient(), API_URL);

    @FXML
    public void handlePrimaryAction() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("login.error.required");
            return;
        }

        doLogin(username, password);
    }

    @FXML
    public void handleSwitchContext() {
        SceneManager.loadRegister();
    }

    private void doLogin(String loginInput, String password) {
        try {
            LoginResult result = authService.login(loginInput, password);

            if (!result.isSuccess()) {
                showError(result.getErrorKey(), result.getErrorArgs());
                return;
            }

            showMessage("login.success.login");

            SessionManager.setSession(
                    result.getUsername(),
                    result.getEmail(),
                    result.getToken(),
                    result.getUserType()
            );

            SceneManager.loadHome();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            showError("login.error.interrupted", e.getMessage());
        } catch (IOException e) {
            showError("login.error.connection", e.getMessage());
        }
    }

    private void showError(String key, Object... args) {
        errorLabel.setText(LocaleManager.getString(key, args));
    }

    private void showMessage(String key, Object... args) {
        errorLabel.setText(LocaleManager.getString(key, args));
    }
}