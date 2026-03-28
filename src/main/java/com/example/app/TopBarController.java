package com.example.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;

import java.util.Locale;

public class TopBarController {

    @FXML
    private ImageView learningPlatformImage;

    @FXML
    private ChoiceBox<Locale> languageChoiceBox;

    @FXML
    private Button profileButton;

    @FXML
    private void initialize() {
        if (languageChoiceBox != null) {
            languageChoiceBox.getItems().setAll(Locale.ENGLISH, Locale.FRENCH, new Locale("fa"));
            languageChoiceBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(Locale locale) {
                    if (locale == null) {
                        return "";
                    }
                    if ("fa".equals(locale.getLanguage())) {
                        return "فارسی";
                    }
                    if (Locale.FRENCH.equals(locale)) {
                        return "Français";
                    }
                    return "English";
                }

                @Override
                public Locale fromString(String string) {
                    if (string == null) {
                        return Locale.ENGLISH;
                    }
                    if (string.equals("فارسی")) {
                        return new Locale("fa");
                    }
                    if (string.equals("Français")) {
                        return Locale.FRENCH;
                    }
                    return Locale.ENGLISH;
                }
            });
            languageChoiceBox.setValue(LocaleManager.getLocale());
        }

        // Update button states after a short delay to allow scene to fully load
        Platform.runLater(this::updateButtonStates);
    }

    @FXML
    public void handleLanguageChange() {
        if (languageChoiceBox == null) {
            return;
        }

        Locale selected = languageChoiceBox.getValue();
        if (selected != null && !selected.equals(LocaleManager.getLocale())) {
            LocaleManager.setLocale(selected);
            SceneManager.reloadCurrentScene();
        }
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
                profileButton.setText(LocaleManager.getBundle().getString("topbar.profile"));
            } else {
                profileButton.setText(LocaleManager.getBundle().getString("topbar.login"));
            }
            profileButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;");
        }
    }

    // Method to allow external updates to button states
    public void refreshButtonStates() {
        updateButtonStates();
    }
}

