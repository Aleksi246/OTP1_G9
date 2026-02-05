// src/main/java/com/example/app/Main.java
package com.example.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/app/main.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("JavaFX Frontend");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}