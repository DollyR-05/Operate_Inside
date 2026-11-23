package com.example.demo;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

public class BrowserController {

    @FXML private WebView webView;
    @FXML private TextField addressBar;
    @FXML private Button backButton, forwardButton, refreshButton, homeButton, goButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;

    private WebEngine webEngine;
    private WebHistory history;

    private static final String HOME_URL = "https://www.google.com";

    @FXML
    public void initialize() {
        webEngine = webView.getEngine();
        history = webEngine.getHistory();

        // Load home page
        loadPage(HOME_URL);

        // Bind progress bar to page loading progress
        progressBar.progressProperty().bind(webEngine.getLoadWorker().progressProperty());

        // Update status label and address bar dynamically
        webEngine.locationProperty().addListener((obs, oldLoc, newLoc) -> addressBar.setText(newLoc));
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            statusLabel.setText(newState.toString());
            backButton.setDisable(history.getCurrentIndex() <= 0);
            forwardButton.setDisable(history.getCurrentIndex() >= history.getEntries().size() - 1);
        });
    }

    private void loadPage(String url) {
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        webEngine.load(url);
    }

    // Event handlers
    @FXML
    private void handleGo() {
        loadPage(addressBar.getText().trim());
    }

    @FXML
    private void handleHome() {
        loadPage(HOME_URL);
    }

    @FXML
    private void handleRefresh() {
        webEngine.reload();
    }

    @FXML
    private void handleBack() {
        if (history.getCurrentIndex() > 0) {
            history.go(-1);
        }
    }

    @FXML
    private void handleForward() {
        if (history.getCurrentIndex() < history.getEntries().size() - 1) {
            history.go(1);
        }
    }
}
