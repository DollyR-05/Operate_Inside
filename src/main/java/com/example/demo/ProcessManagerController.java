package com.example.demo;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessManagerController {

    @FXML private TextField processNameField;
    @FXML private Button startBtn;
    @FXML private Button terminateBtn;
    @FXML private Button shutdownBtn;

    @FXML private TableView<Process> processTable;
    @FXML private TableColumn<Process, Integer> pidColumn;
    @FXML private TableColumn<Process, String> nameColumn;
    @FXML private TableColumn<Process, String> stateColumn;
    @FXML private TableColumn<Process, Long> cpuTimeColumn;

    @FXML private Label statusLabel;
    @FXML private Pane ganttChartPane;

    private final ObservableList<Process> processList = javafx.collections.FXCollections.observableArrayList();
    private final Queue<Process> readyQueue = new LinkedList<>();
    private Process runningProcess = null;

    private final AtomicInteger pidGenerator = new AtomicInteger(1);
    private final long TIME_SLICE_MS = 1000;  // 1 second time slice
    private volatile boolean schedulerRunning = true;

    private final List<ScheduleInterval> scheduleIntervals = new ArrayList<>();
    private long totalElapsedTime = 0;

    private final Map<Integer, Color> processColors = new HashMap<>();
    private final List<Color> availableColors = Arrays.asList(
            Color.web("#00bcd4"), Color.web("#ff9800"), Color.web("#4caf50"), Color.web("#f44336"),
            Color.web("#2196f3"), Color.web("#e91e63"), Color.web("#ffeb3b"), Color.web("#9c27b0")
    ); // Using web colors for consistency
    private int colorIndex = 0;

    @FXML
    public void initialize() {
        pidColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getPid()));
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());
        stateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getState().toString()));
        cpuTimeColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getCpuTime()));

        processTable.setItems(processList);

        startScheduler();
    }

    @FXML
    private void handleStart() {
        String name = processNameField.getText();
        if (name == null || name.isBlank()) {
            statusLabel.setText("Process name required.");
            return;
        }

        int pid = pidGenerator.getAndIncrement();
        Process newProcess = new Process(pid, name, 0);
        newProcess.setState(Process.State.READY);

        // Assign color
        synchronized (processColors) {
            Color assignedColor = availableColors.get(processColors.size() % availableColors.size());
            processColors.put(pid, assignedColor);
        }

        Platform.runLater(() -> {
            processList.add(newProcess);
            synchronized (readyQueue) {
                readyQueue.offer(newProcess);
            }
            statusLabel.setText("Process started: " + name);
            processNameField.clear();
            updateGanttChartWithIntervals();
        });
    }

    @FXML
    private void handleTerminateProcess() {
        Process selected = processTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Please select a process to terminate.");
            return;
        }

        selected.setState(Process.State.TERMINATED);

        synchronized (readyQueue) {
            readyQueue.remove(selected);
        }
        if (runningProcess == selected) {
            runningProcess = null;
        }

        Platform.runLater(() -> {
            processList.remove(selected);
            processTable.refresh();
            statusLabel.setText("Terminated process: " + selected.getName());
            // No need to call updateGantt here, as the scheduler will update it after the next run.
        });
    }

    @FXML
    private void handleShutdown() {
        schedulerRunning = false;

        synchronized (readyQueue) {
            readyQueue.clear();
        }
        runningProcess = null;

        Platform.runLater(() -> {
            processList.clear();
            processTable.refresh();
            ganttChartPane.getChildren().clear();
            statusLabel.setText("System shutdown: All processes terminated and scheduler stopped.");
            startBtn.setDisable(true);
            terminateBtn.setDisable(true);
            shutdownBtn.setDisable(true);
            processNameField.setDisable(true);
        });
    }

    private void startScheduler() {
        Thread schedulerThread = new Thread(() -> {
            while (schedulerRunning) {
                if (runningProcess == null) {
                    Process nextProcess;
                    synchronized (readyQueue) {
                        nextProcess = readyQueue.poll();
                    }
                    if (nextProcess != null) {
                        runningProcess = nextProcess;
                        runProcess(nextProcess);
                    } else {
                        sleep(1000);
                    }
                } else {
                    sleep(1000);
                }
            }
        });
        schedulerThread.setDaemon(true);
        schedulerThread.start();
    }

    private void runProcess(Process process) {
        process.setState(Process.State.RUNNING);

        Platform.runLater(processTable::refresh);
        long executedCpuTime = process.getCpuTime();

        try {
            Thread.sleep(TIME_SLICE_MS);
        } catch (InterruptedException ignored) {}

        long updatedCpuTime = executedCpuTime + TIME_SLICE_MS;

        // Only commit changes if the process wasn't externally terminated during sleep
        if (schedulerRunning && process.getState() == Process.State.RUNNING) {
            process.setCpuTime(updatedCpuTime);

            final long sliceStart = totalElapsedTime;
            totalElapsedTime += TIME_SLICE_MS;

            synchronized (scheduleIntervals) {
                scheduleIntervals.add(new ScheduleInterval(process, sliceStart, TIME_SLICE_MS));
            }

            Platform.runLater(() -> {
                process.setState(Process.State.READY);
                synchronized (readyQueue) {
                    readyQueue.offer(process);
                }
                runningProcess = null;
                processTable.refresh();
                updateStatusLabel();
                updateGanttChartWithIntervals(); // Update Gantt chart after slice completion
            });
        } else {
            // Process was terminated externally during the sleep or shutdown occurred
            runningProcess = null;
        }
    }

    private void updateStatusLabel() {
        statusLabel.setText("Scheduler active. Last update: " + java.time.LocalTime.now().withNano(0));
    }

    private void updateGanttChartWithIntervals() {
        Platform.runLater(() -> {
            ganttChartPane.getChildren().clear();
            if (scheduleIntervals.isEmpty()) return;

            // Get current dimensions, using fallback if not yet displayed
            double width = ganttChartPane.getWidth() > 0 ? ganttChartPane.getWidth() : 700;
            double barHeight = 40;

            long lastIntervalEnd = scheduleIntervals.get(scheduleIntervals.size() - 1).getStartTime()
                    + scheduleIntervals.get(scheduleIntervals.size() - 1).getDuration();
            long totalTime = (long) (Math.ceil(lastIntervalEnd / 1000.0) * 1000);

            double scale = width / (double) totalTime;

            // Draw Time Axis
            Line axis = new Line(0, barHeight + 20, width, barHeight + 20);
            axis.setStroke(Color.web("#e8e8e8")); // Changed axis color to contrast
            ganttChartPane.getChildren().add(axis);

            // Draw Time Ticks
            int ticks = (int) (totalTime / 1000);
            for (int i = 0; i <= ticks; i++) {
                double x = i * 1000 * scale;
                Line tick = new Line(x, barHeight + 20, x, barHeight + 26);
                tick.setStroke(Color.web("#e8e8e8"));
                Text label = new Text(x - 8, barHeight + 40, i + "s");
                label.setFill(Color.web("#e8e8e8")); // Set time label color
                ganttChartPane.getChildren().addAll(tick, label);
            }

            // Draw Process Rectangles
            for (ScheduleInterval interval : scheduleIntervals) {
                double x = interval.getStartTime() * scale;
                double w = interval.getDuration() * scale;

                Color fillColor = processColors.getOrDefault(interval.getProcess().getPid(), Color.GRAY);

                Rectangle rect = new Rectangle(x, 10, w, barHeight);
                rect.setFill(fillColor);
                rect.setArcWidth(4);
                rect.setArcHeight(4);

                Text label = new Text(x + 5, 35, "P" + interval.getProcess().getPid());
                label.setFill(Color.web("#1a1a2e")); // Dark text for contrast on colored bar

                ganttChartPane.getChildren().addAll(rect, label);
            }
        });
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) { }
    }

    public void shutdown() {
        schedulerRunning = false;
    }


    private static class ScheduleInterval {
        private final Process process;
        private final long startTime; // ms
        private final long duration;  // ms

        public ScheduleInterval(Process process, long startTime, long duration) {
            this.process = process;
            this.startTime = startTime;
            this.duration = duration;
        }

        public Process getProcess() {
            return process;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getDuration() {
            return duration;
        }
    }
}