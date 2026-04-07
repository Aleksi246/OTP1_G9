package com.example.app;

import java.util.Locale;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.util.StringConverter;

public class TopBarController {

  @FXML
  private ChoiceBox<Locale> languageChoiceBox;

  @FXML
  private Button profileButton;

  @FXML
  private void initialize() {
    if (languageChoiceBox != null) {
      languageChoiceBox
              .getItems()
              .setAll(Locale.ENGLISH,
                      Locale.FRENCH,
                      new Locale("fa"),
                      new Locale("ru"));
      languageChoiceBox.setConverter(new StringConverter<>() {
        @Override
        public String toString(Locale locale) {
          if (locale == null) return "";
          return switch (locale.getLanguage()) {
            case "fa" -> "فارسی";
            case "ru" -> "Русский";
            case "fr" -> "Français";
            default -> "English";
          };
        }

        @Override
        public Locale fromString(String string) {
          return Locale.ENGLISH;
        }
      });
      languageChoiceBox.setValue(LocaleManager.getLocale());
    }

    Platform.runLater(this::updateButtonStates);
  }

  @FXML
  public void handleLanguageChange() {
    if (languageChoiceBox == null) return;
    Locale selected = languageChoiceBox.getValue();
    if (selected != null && !selected.equals(LocaleManager.getLocale())) {
      LocaleManager.setLocale(selected);
      SceneManager.reloadCurrentScene();
    }
  }

  @FXML
  public void handleLearningPlatformClick() {
    if (SessionManager.isLoggedIn()) {
      SceneManager.loadHome();
    }
  }

  @FXML
  public void handleSwitchToProfile() {
    if (SessionManager.isLoggedIn()) {
      SceneManager.loadProfile();
    } else {
      SceneManager.loadLogin();
    }
  }

  private void updateButtonStates() {
    if (profileButton != null) {
      String key = SessionManager.isLoggedIn() ? "topbar.profile" : "topbar.login";
      profileButton.setText(LocaleManager.getBundle().getString(key));
    }
  }
}
