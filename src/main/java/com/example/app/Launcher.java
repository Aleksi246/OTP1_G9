package com.example.app;

/**
 * Non-JavaFX entry point for the fat JAR.
 * Required because Main extends Application, and JavaFX enforces
 * module-system checks when the main class is an Application subclass.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
