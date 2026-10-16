package com.cqu.drs.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JavaFX client of DRS-Enhanced (run the server first). Shows the login
 * screen, then the main window. Views are FXML files designed in Scene Builder
 * (View), controllers live in {@code com.cqu.drs.client.controller} (Controller),
 * and the model is the shared domain model served by the server (Model).
 */
public class ClientApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        stage.setTitle("Disaster Response System - DRS-Enhanced");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        showLogin();
        stage.show();
    }

    /** Shows the login screen. */
    public static void showLogin() throws IOException {
        setRoot("/com/cqu/drs/client/view/LoginView.fxml", 1180, 760);
    }

    /** Shows the main application window after a successful login. */
    public static void showMain() throws IOException {
        setRoot("/com/cqu/drs/client/view/MainView.fxml", 1280, 800);
        primaryStage.setMaximized(true);
    }

    private static void setRoot(String fxml, double w, double h) throws IOException {
        Parent root = FXMLLoader.load(ClientApp.class.getResource(fxml));
        if (primaryStage.getScene() != null) {
            // keep the window size/maximised state when switching between login and main views
            primaryStage.getScene().setRoot(root);
            return;
        }
        Scene scene = new Scene(root, w, h);
        scene.getStylesheets().add(ClientApp.class.getResource("/com/cqu/drs/client/view/drs.css").toExternalForm());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F12) {
                saveScreenshot(scene);
            }
        });
        primaryStage.setScene(scene);
    }

    /** F12: saves a PNG of the current window to ./screenshots (used to document the system). */
    private static void saveScreenshot(Scene scene) {
        try {
            WritableImage img = scene.snapshot(null);
            Path dir = Paths.get("screenshots");
            Files.createDirectories(dir);
            String name = "drs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".png";
            ImageIO.write(SwingFxBridge.toBuffered(img), "png", dir.resolve(name).toFile());
            System.out.println("Screenshot saved: " + dir.resolve(name).toAbsolutePath());
        } catch (IOException | RuntimeException ex) {
            System.err.println("Screenshot failed: " + ex.getMessage());
        }
    }

    @Override
    public void stop() {
        if (ClientSession.getConnection() != null) {
            ClientSession.getConnection().logoutAndClose();
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
