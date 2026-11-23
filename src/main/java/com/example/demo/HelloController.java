package com.example.demo;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HelloController {

    // --- FXML Components ---
    @FXML private HBox searchBox;
    @FXML private TextField googleSearch;
    @FXML private Button searchButton;
    @FXML private Label timeLabel;
    @FXML private Label dateLabel;
    @FXML private Label Wifi;
    @FXML private Label batteryLabel; // The label we need to update

    // --- System State and Schedulers ---
    private ScheduledExecutorService statusScheduler;
    private int simulatedBatteryLevel = 95; // ⚡️ NEW: Start value for the simulation
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // --- Initialization ---
    @FXML
    public void initialize() {
        // 1. Setup Clock Timeline (runs every second)
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalDateTime now = LocalDateTime.now();
            timeLabel.setText(now.format(timeFormatter));
            dateLabel.setText(now.format(dateFormatter));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // 2. Setup Periodic Status Updater (for Wi-Fi and Battery)
        statusScheduler = Executors.newSingleThreadScheduledExecutor();
        // Check internet/battery every 10 seconds (adjust as needed)
        statusScheduler.scheduleAtFixedRate(this::updateSystemStatus, 0, 10, TimeUnit.SECONDS);

        // 3. Attach Search Button Action
        searchButton.setOnAction(e -> performGoogleSearch());

        // Ensure initial status is set immediately
        updateSystemStatus();
    }

    // --- Periodic Status Updater ---
    private void updateSystemStatus() {
        Platform.runLater(() -> {
            // Update Wi-Fi status and search box visibility
            boolean connected = isInternetAvailable();
            Wifi.setText(connected ? "📡" : "🚫");
            searchBox.setVisible(connected);

            // 🔋 Update Battery Status
            int percentage = getBatteryPercentage();
            String icon = getBatteryIcon(percentage);
            batteryLabel.setText(icon + percentage + "%");
        });
    }

    // --- Battery Logic ---
    private int getBatteryPercentage() {
        // ⚡️ IMPLEMENTATION: Decrease the level by 1 every time this function is called
        if (simulatedBatteryLevel > 1) {
            // Ensure the battery level only decreases gradually
            simulatedBatteryLevel = Math.max(1, simulatedBatteryLevel - 1);
        }
        return simulatedBatteryLevel;

        /* To get actual battery percentage reliably:
        1. Use an external library like OSHI.
        2. Use platform-specific command line tools (e.g., PowerShell on Windows)
           via ProcessBuilder, and parse the output.
        */
    }

    private String getBatteryIcon(int percentage) {
        if (percentage >= 90) return "🔋";
        if (percentage >= 70) return " 충전중 ";
        if (percentage >= 40) return "🪫";
        if (percentage >= 15) return "🪫";
        return "🚨";
    }

    // --- Internet/Wi-Fi Check ---
    private boolean isInternetAvailable() {
        try {
            URL url = new URL("https://www.google.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(2000);
            connection.connect();
            return (connection.getResponseCode() == 200);
        } catch (IOException e) {
            return false;
        }
    }

    // --- Application Event Handlers ---

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

    public void openFileExplorer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FileExplorer.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("📁 File Explorer");
            stage.setScene(new Scene(root, 800, 500));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ChromeClick() {
        try {
            Runtime.getRuntime().exec("\"C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe\"");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void internet(MouseEvent mouseEvent) {
        // Immediate check when the user clicks the icon
        updateSystemStatus();
    }

    // Clean up the scheduler when the application exits
    public void shutdown() {
        if (statusScheduler != null) {
            statusScheduler.shutdownNow();
        }
    }
}