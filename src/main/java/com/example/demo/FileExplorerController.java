package com.example.demo;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

public class FileExplorerController {

    @FXML
    private ListView<String> fileList;

    @FXML
    private HBox breadcrumbBar; // navigation bar

    private File currentDir;



    private Stack<File> backHistory = new Stack<>();
    private Stack<File> forwardHistory = new Stack<>();
    @FXML

    public void initialize() {
        String homePath = System.getProperty("user.home");
        if (homePath == null) {
            System.err.println("Home directory not found!");
            return;
        }

        File homeDir = new File(homePath);
        if (!homeDir.exists()) {
            System.err.println("Invalid home directory: " + homeDir.getAbsolutePath());
            return;
        }

        File[] files = homeDir.listFiles();
        if (files == null) {
            System.err.println("No files in: " + homeDir.getAbsolutePath());
            return;
        }

        // Now safely populate your ListView
        for (File file : files) {
            fileList.getItems().add(file.getName());
        }
        currentDir = homeDir;

        listFiles(currentDir);
        updateBreadcrumb();
    }


    private void listFiles(File dir) {
        fileList.getItems().clear();

        if (dir == null || !dir.exists()) {
            fileList.getItems().add("Directory does not exist.");
            return;
        }

        File[] files = dir.listFiles(f ->
                !f.isHidden() && f.canRead() && !f.getName().startsWith(".")
        );

        if (files == null || files.length == 0) {
            fileList.getItems().add("Folder is empty."); // ✅ show empty message
            return;
        }

        Arrays.sort(files, Comparator
                .comparing(File::isFile)
                .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        for (File f : files) {
            String prefix = f.isDirectory() ? "📁 " : "📄 ";
            fileList.getItems().add(prefix + f.getName());
        }
    }


    @FXML
    private void handleDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            String selected = fileList.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            String fileName = selected.substring(2).trim();
            File selectedFile = new File(currentDir, fileName);

            if (selectedFile.isDirectory()) {
                backHistory.push(currentDir);  // save current for forward
                currentDir = selectedFile;
                listFiles(currentDir);
                updateBreadcrumb();
                forwardHistory.clear(); // new navigation invalidates forward history

            } else {
                showFileInfo(selectedFile);
            }
        }
    }

    private void updateBreadcrumb() {
        breadcrumbBar.getChildren().clear();

        if (currentDir == null) return;

        File temp = currentDir;
        String[] parts = temp.getAbsolutePath().split("[/\\\\]+"); // ✅ fixed split
        StringBuilder pathBuilder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            pathBuilder.append(part);

            File target = new File(pathBuilder.toString());
            Button crumb = new Button(part);
            crumb.setStyle("-fx-background-color: #3c3f41; -fx-text-fill: white; -fx-font-size: 12;");
            crumb.setOnAction(e -> {
                currentDir = target;
                updateBreadcrumb();
                listFiles(currentDir);
            });

            breadcrumbBar.getChildren().add(crumb);

            if (i < parts.length - 1) {
                Label sep = new Label(" > ");
                sep.setStyle("-fx-text-fill: lightgray;");
                breadcrumbBar.getChildren().add(sep);
            }

            pathBuilder.append(File.separator);
        }
    }

    private void showFileInfo(File file) {
        Stage stage = new Stage();
        VBox box = new VBox(10);
        box.setStyle("-fx-padding: 20; -fx-background-color: #2b2b2b;");

        Label name = new Label("📄 " + file.getName());
        Label path = new Label("Path: " + file.getAbsolutePath());
        Label size = new Label("Size: " + file.length() + " bytes");

        name.setStyle("-fx-text-fill: white;");
        path.setStyle("-fx-text-fill: white;");
        size.setStyle("-fx-text-fill: white;");

        box.getChildren().addAll(name, path, size);

        stage.setScene(new Scene(box, 400, 150));
        stage.setTitle("File Info");
        stage.show();
    }

    @FXML
    private void goBack() {
        if (!backHistory.isEmpty()) {
            forwardHistory.push(currentDir);  // save current for Forward
            currentDir = backHistory.pop();   // go to previous folder
            listFiles(currentDir);
            updateBreadcrumb();
        }
    }

    @FXML
    private void goForward() {
        if (!forwardHistory.isEmpty()) {
            backHistory.push(currentDir);      // save current for Back
            currentDir = forwardHistory.pop(); // go to next folder
            listFiles(currentDir);
            updateBreadcrumb();
        }
    }


}
