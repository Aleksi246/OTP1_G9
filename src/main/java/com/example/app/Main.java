// src/main/java/com/example/app/Main.java
package com.example.app;

import com.example.otp.dao.UserDao;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;
import com.example.otp.model.User;

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
        UserDao userDao = new UserDao();
        // Just a quick test to ensure DB connection works at startup. Remove in production.
        try {
            List<User> list = userDao.findAll();
            for (User u : list) {
                System.out.println("User: " + u.getUsername());
            }
            System.out.println("DB connection successful.");
        } catch (Exception e) {
            System.err.println("DB connection failed: " + e.getMessage());
        }
        launch(args);
    }
}