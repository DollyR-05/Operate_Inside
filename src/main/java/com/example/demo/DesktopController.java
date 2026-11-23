package com.example.demo;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DesktopController {

    // FXML fields for Taskbar
    @FXML private HBox appTabs;
    @FXML private HBox searchBox;
    @FXML private TextField googleSearch;
    @FXML private Button searchButton;
    @FXML private Label timeLabel;
    @FXML private Label dateLabel;
    @FXML private Label Wifi;

    // Field necessary to hide the "Google Search" text in the FXML
    @FXML
    private VBox googleSearchContainer;

    private ListView<String> fileListD;

    @FXML
    public void initialize() {
        // --- 1. MINIMALIST TASKBAR: Hide unnecessary elements ---
        if (appTabs != null) {
            appTabs.setVisible(false);
            appTabs.setManaged(false);
        }

        // Hides the entire Google Search VBox container (Label and HBox)
        if (googleSearchContainer != null) {
            googleSearchContainer.setVisible(false);
            googleSearchContainer.setManaged(false);
        }

        // --- 2. CLOCK AND DATE ---
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalDateTime now = LocalDateTime.now();
            timeLabel.setText(now.format(timeFormatter));
            dateLabel.setText(now.format(dateFormatter));
        }), new KeyFrame(Duration.seconds(1)));

        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // --- 3. SEARCH LOGIC (Kept for compilation) ---
        searchButton.setOnAction(e -> performGoogleSearch());
    }

    // --- HELPER METHODS ---
    private void performGoogleSearch() {
        String query = googleSearch.getText().trim();
        if (query.isEmpty()) return;

        try {
            String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
            java.awt.Desktop.getDesktop().browse(
                    new URL("https://www.google.com/search?q=" + encodedQuery).toURI()
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private boolean isInternetAvailable() {
        try {
            URL url = new URL("https://www.google.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(200);
            connection.connect();
            return (connection.getResponseCode() == 200);
        } catch (IOException e) {
            return false;
        }
    }

    // --- APPLICATION LAUNCH METHODS (Connected to Desktop Icons) ---

    @FXML // Connected to File Explorer desktop icon
    public void openFileExplorer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FileExplorer.fxml"));
            Parent root = loader.load();
            java.net.URL cssUrl = getClass().getResource("FileExplorer.css");
            Stage stage = new Stage();
            stage.setTitle("📁 File Explorer");
            Scene scene = new Scene(root, 800, 500);
            if (cssUrl != null) {
                String cssPath = cssUrl.toExternalForm();
                scene.getStylesheets().add(cssPath);
            }

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML // Connected to Browssy desktop icon
    public void ChromeClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Browser.fxml"));
            Parent root = loader.load();
            java.net.URL cssUrl = getClass().getResource("FileExplorer.css");
            Stage stage = new Stage();
            stage.setTitle("📁 Browssy");
            Scene scene = new Scene(root, 800, 500);
            if (cssUrl != null) {
                String cssPath = cssUrl.toExternalForm();
                scene.getStylesheets().add(cssPath);
            }
            stage.setScene(scene);
            stage.show();} catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML // Connected to Calculator desktop icon
    public void openCalculator() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Calculator.fxml"));
            Parent root = loader.load();

            java.net.URL cssUrl = getClass().getResource("Calculator.css");



            String cssPath = cssUrl.toExternalForm();

            Stage stage = new Stage();
            stage.setTitle("Stack-Based Calculator");

            Scene scene = new Scene(root);

            scene.getStylesheets().add(cssPath);

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML // Connected to Scheduler desktop icon (Placeholder)
    public void openProcessManager() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ProcessManager.fxml"));
            Parent root = loader.load();
            java.net.URL cssUrl = getClass().getResource("FileExplorer.css");
            Stage stage = new Stage();
            stage.setTitle("📁 Process Manager");
            Scene scene = new Scene(root, 800, 500);
            if (cssUrl != null) {
                String cssPath = cssUrl.toExternalForm();
                scene.getStylesheets().add(cssPath);
            }
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }}

    @FXML // Connected to Wifi icon
    public void internet(MouseEvent mouseEvent) {
        boolean connecte = isInternetAvailable();
        if(connecte){
            Wifi.setText("\uD83D\uDCF6");
        }
        else Wifi.setText("🚫");
    }
}