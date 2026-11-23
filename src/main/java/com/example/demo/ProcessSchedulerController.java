package com.example.demo;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessSchedulerController {

    // --- FXML Components ---
    @FXML private TextField processNameField;
    @FXML private TextField burstTimeField;
    @FXML private TextField parameterField;
    @FXML private ComboBox<String> algorithmComboBox;
    @FXML private ComboBox<String> timeUnitComboBox;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;
    @FXML private Button terminateBtn;
    @FXML private Button shutdownRefreshBtn;
    @FXML private Button deleteSelectedBtn;
    @FXML private TableView<SProcess> processTable;
    @FXML private TableColumn<SProcess, Integer> pidColumn;
    @FXML private TableColumn<SProcess, String> nameColumn;
    @FXML private TableColumn<SProcess, String> stateColumn;
    @FXML private TableColumn<SProcess, Long> burstTimeColumn;
    @FXML private TableColumn<SProcess, Long> cpuTimeColumn;
    @FXML private TableColumn<SProcess, Long> arrivalColumn;
    @FXML private TableColumn<SProcess, String> responseColumn;
    @FXML private TableColumn<SProcess, String> turnaroundColumn;
    @FXML private TableColumn<SProcess, String> completionColumn;
    @FXML private Label statusLabel;
    @FXML private Pane ganttChartPane;

    // --- Data Structures and State ---
    private final ObservableList<SProcess> processList = FXCollections.observableArrayList();
    private final Queue<SProcess> readyQueue = new LinkedList<>();
    private final AtomicInteger pidGenerator = new AtomicInteger(1);

    private volatile boolean schedulerRunning = false;
    private volatile boolean schedulerPaused = false;
    private long timeSliceMs = 1000;
    private long totalElapsedTime = 0;
    private Thread schedulerThread;

    // Process Colors: SMOOTHER PALETTE 🎨
    private final List<Color> availableColors = Arrays.asList(
            Color.web("#80DEEA"), // Light Cyan
            Color.web("#B39DDB"), // Light Purple
            Color.web("#FFCC80"), // Light Orange
            Color.web("#A5D6A7"), // Light Green
            Color.web("#EF9A9A"), // Light Red
            Color.web("#90CAF9"), // Light Blue
            Color.web("#F48FB1"), // Light Pink
            Color.web("#FFF59D")  // Light Yellow
    );
    private final List<ScheduleInterval> scheduleIntervals = Collections.synchronizedList(new ArrayList<>());
    private final Map<Integer, Color> processColors = Collections.synchronizedMap(new HashMap<>());
    private int colorIndex = 0;

    // Time Unit Management
    private enum TimeUnit { MILLISECONDS, SECONDS }
    private TimeUnit currentUnit = TimeUnit.SECONDS;
    private final long SECOND_IN_MS = 1000;


    // --- Initialization ---
    @FXML
    public void initialize() {
        pidColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getPid()));
        nameColumn.setCellValueFactory(d -> d.getValue().nameProperty());
        stateColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getState().toString()));

        burstTimeColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(convertTimeForDisplay(d.getValue().getBurstTime())));
        cpuTimeColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(convertTimeForDisplay(d.getValue().getCpuTime())));
        arrivalColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(convertTimeForDisplay(d.getValue().getArrivalTime())));

        // Ensure Response, Turnaround, Completion use the formatTime helper
        responseColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(formatTime(d.getValue().getResponseTime())));
        turnaroundColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(formatTime(d.getValue().getTurnaroundTime())));
        completionColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(formatTime(d.getValue().getCompletionTime())));

        processTable.setItems(processList);

        algorithmComboBox.getItems().addAll("ROUND_ROBIN", "FCFS", "SJF");
        algorithmComboBox.getSelectionModel().selectFirst();

        timeUnitComboBox.getItems().addAll("Milliseconds", "Seconds");
        timeUnitComboBox.getSelectionModel().select("Seconds");
        timeUnitComboBox.setOnAction(e -> {
            String sel = timeUnitComboBox.getValue();
            currentUnit = sel.equals("Milliseconds") ? TimeUnit.MILLISECONDS : TimeUnit.SECONDS;
            Platform.runLater(() -> {
                processTable.refresh();
                updateGanttChart();
            });
        });

        // Initial sizing for the pane
        ganttChartPane.setPrefWidth(500);
        ganttChartPane.setPrefHeight(90);
    }

    // --- Time Conversion Helpers ---
    private Long convertTimeForDisplay(long timeInMs) {
        if (timeInMs < 0) return null;
        if (currentUnit == TimeUnit.MILLISECONDS) return timeInMs;
        else return timeInMs / SECOND_IN_MS;
    }

    private String formatTime(long timeInMs) {
        if (timeInMs < 0) return "-";
        if (currentUnit == TimeUnit.MILLISECONDS) return String.valueOf(timeInMs);
        else return String.format("%.2f", timeInMs / (double)SECOND_IN_MS);
    }

    // --- UI Event Handlers ---
    @FXML private void handleAddProcess() {
        String name = processNameField.getText();
        if (name == null || name.isBlank()) { statusLabel.setText("Enter a process name."); return; }
        long burst;
        try {
            burst = Long.parseLong(burstTimeField.getText());
            if (burst <= 0) throw new NumberFormatException();
            if (currentUnit == TimeUnit.SECONDS) burst *= SECOND_IN_MS;
        } catch (NumberFormatException e) { statusLabel.setText("Invalid burst time."); return; }

        int pid = pidGenerator.getAndIncrement();
        long arrivalTime = totalElapsedTime;
        SProcess p = new SProcess(pid, name, arrivalTime);
        p.setBurstTime(burst);
        p.setState(SProcess.State.READY);

        // Assign color
        synchronized (processColors) {
            Color color = availableColors.get(colorIndex++ % availableColors.size());
            processColors.put(pid, color);
        }

        processList.add(p);
        synchronized (readyQueue) { readyQueue.offer(p); }

        processNameField.clear();
        burstTimeField.clear();
        statusLabel.setText("Added process: " + name);
    }

    @FXML private void handleTerminateProcess() {
        SProcess sel = processTable.getSelectionModel().getSelectedItem();
        if (sel == null) { statusLabel.setText("Select a process to terminate."); return; }
        if (sel.getState() != SProcess.State.TERMINATED) {
            sel.setState(SProcess.State.TERMINATED);
            // Only update completion time/metrics if the scheduler is running
            if (schedulerRunning) {
                sel.setCompletionTime(totalElapsedTime);
                sel.setTurnaroundTime(sel.getCompletionTime() - sel.getArrivalTime());
                sel.setWaitingTime(sel.getTurnaroundTime() - sel.getBurstTime());
            }

            processTable.refresh();
            synchronized (readyQueue) { readyQueue.removeIf(p -> p.getPid() == sel.getPid()); }
            updateGanttChart();
            statusLabel.setText("Process terminated: " + sel.getName());
        } else { statusLabel.setText("Process already terminated."); }
    }

    @FXML private void handleDeleteSelectedProcess() {
        SProcess sel = processTable.getSelectionModel().getSelectedItem();
        if (sel == null) { statusLabel.setText("Select a process to delete."); return; }

        // Remove intervals and colors associated with the deleted process
        scheduleIntervals.removeIf(si -> si.process != null && si.process.getPid() == sel.getPid());
        processColors.remove(sel.getPid());

        processList.remove(sel);
        synchronized (readyQueue) { readyQueue.remove(sel); }

        updateGanttChart();
        statusLabel.setText("Deleted process: " + sel.getName());
    }

    @FXML private void handleStartScheduling() {
        if (schedulerRunning) { statusLabel.setText("Scheduler already running."); return; }
        long inputTime = 1000;
        try {
            inputTime = Long.parseLong(parameterField.getText());
            if (inputTime <= 0) throw new NumberFormatException();
            if (currentUnit == TimeUnit.SECONDS) inputTime *= SECOND_IN_MS;
        } catch (Exception e) { inputTime = 1000; }
        timeSliceMs = inputTime;

        schedulerRunning = true;
        schedulerPaused = false;
        String algo = algorithmComboBox.getValue();
        statusLabel.setText("Scheduler started: " + algo + (algo.equals("ROUND_ROBIN") ? " (Q=" + formatTime(timeSliceMs) + (currentUnit == TimeUnit.SECONDS ? "s)" : "ms)") : ""));

        schedulerThread = new Thread(() -> runScheduler(algo));
        schedulerThread.setDaemon(true);
        schedulerThread.start();
    }

    @FXML private void handleStopScheduling() {
        if (!schedulerRunning) { statusLabel.setText("Scheduler not running."); return; }
        schedulerPaused = !schedulerPaused;
        stopBtn.setText(schedulerPaused ? "Resume" : "Pause");
        statusLabel.setText(schedulerPaused ? "Scheduler paused." : "Scheduler resumed.");
    }

    @FXML private void handleShutdownRefresh() {
        schedulerRunning = false;
        schedulerPaused = false;
        if (schedulerThread != null) schedulerThread.interrupt();
        processList.clear();
        synchronized (readyQueue) { readyQueue.clear(); }
        scheduleIntervals.clear();
        processColors.clear();
        colorIndex = 0;
        totalElapsedTime = 0;
        ganttChartPane.getChildren().clear();
        statusLabel.setText("System reset.");
        stopBtn.setText("Pause");
    }

    // --- Scheduling Logic ---
    private void runScheduler(String algo) {
        while (schedulerRunning) {
            if (schedulerPaused) { sleep(100); continue; }
            SProcess next = null;
            synchronized (readyQueue) {
                if (!readyQueue.isEmpty()) {
                    switch (algo) {
                        case "ROUND_ROBIN", "FCFS" -> next = readyQueue.poll();
                        case "SJF" -> {
                            next = readyQueue.stream()
                                    .filter(p -> p.getState() == SProcess.State.READY)
                                    .min(Comparator.comparingLong(SProcess::getBurstTime)) // Simple SJF uses full burst time
                                    .orElse(null);
                            if (next != null) readyQueue.remove(next);
                        }
                    }
                }
            }

            if (next == null) {
                boolean allDone = processList.stream().allMatch(p -> p.getState() == SProcess.State.TERMINATED);
                if (allDone && !processList.isEmpty()) {
                    schedulerRunning = false;
                    Platform.runLater(() -> statusLabel.setText("Scheduling finished."));
                    break;
                }
                long idleDuration = 100;
                recordIdle(idleDuration);
                sleep(idleDuration);
                continue;
            }

            if (next.getState() != SProcess.State.READY) { continue; }

            if (next.getStartTime() == -1) {
                next.setStartTime(totalElapsedTime);
                next.setResponseTime(totalElapsedTime - next.getArrivalTime());
            }
            next.setState(SProcess.State.RUNNING);

            if ("ROUND_ROBIN".equals(algo)) {
                runRoundRobin(next);
            } else {
                runNonPreemptive(next);
            }
            Platform.runLater(processTable::refresh);
        }
    }

    private void runRoundRobin(SProcess p) {
        long remain = p.getBurstTime() - p.getCpuTime();
        long exec = Math.min(remain, timeSliceMs);
        executeProcess(p, exec);

        long currentRemain = p.getBurstTime() - p.getCpuTime();

        // Check if terminated during execution (e.g., from UI) or finished
        if (p.getState() == SProcess.State.TERMINATED || currentRemain <= 0) {
            p.setState(SProcess.State.TERMINATED);
            if (p.getCompletionTime() == -1) {
                p.setCompletionTime(totalElapsedTime);
                p.setTurnaroundTime(p.getCompletionTime() - p.getArrivalTime());
                p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());
            }
        } else {
            p.setState(SProcess.State.READY);
            synchronized (readyQueue) { readyQueue.offer(p); }
        }
    }

    private void runNonPreemptive(SProcess p) {
        long remain = p.getBurstTime() - p.getCpuTime();
        executeProcess(p, remain);

        // Check if terminated during execution (e.g., from UI) or finished
        if (p.getState() == SProcess.State.TERMINATED || p.getCpuTime() >= p.getBurstTime()) {
            p.setState(SProcess.State.TERMINATED);
            if (p.getCompletionTime() == -1) {
                p.setCompletionTime(totalElapsedTime);
                p.setTurnaroundTime(p.getCompletionTime() - p.getArrivalTime());
                p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());
            }
        } else {
            p.setState(SProcess.State.READY);
            // This case should ideally not happen for non-preemptive but is here for robustness
            synchronized (readyQueue) { readyQueue.offer(p); }
        }
    }

    private void executeProcess(SProcess p, long duration) {
        long elapsed = 0;
        long sleepInterval = 50; // Granularity of updates

        while (elapsed < duration && schedulerRunning && !schedulerPaused) {
            if (p.getState() != SProcess.State.RUNNING) { break; }

            long step = Math.min(sleepInterval, duration - elapsed);

            sleep(step);
            elapsed += step;
            totalElapsedTime += step;

            p.setCpuTime(Math.min(p.getCpuTime() + step, p.getBurstTime()));
            recordInterval(p, totalElapsedTime - step, step);

            // Refresh table periodically
            if (elapsed % (sleepInterval * 4) == 0) { Platform.runLater(processTable::refresh); }
        }

        Platform.runLater(this::updateGanttChart);
    }

    private void recordInterval(SProcess p, long start, long dur) {
        if (dur <= 0) return;
        scheduleIntervals.add(new ScheduleInterval(p, start, dur));
    }

    private void recordIdle(long dur) {
        if (dur <= 0) return;

        // Merge with last idle interval if one exists
        if (!scheduleIntervals.isEmpty()) {
            ScheduleInterval last = scheduleIntervals.get(scheduleIntervals.size() - 1);
            if (last.process == null) {
                last.duration += dur;
                totalElapsedTime += dur;
                Platform.runLater(this::updateGanttChart);
                return;
            }
        }
        scheduleIntervals.add(new ScheduleInterval(null, totalElapsedTime, dur));
        totalElapsedTime += dur;
        Platform.runLater(this::updateGanttChart);
    }

    // --- CORRECTED AND STYLED Gantt Chart Method ---
    private void updateGanttChart() {
        ganttChartPane.getChildren().clear();
        if (scheduleIntervals.isEmpty()) return;

        final double height = 40;
        final double scaleFactor = 0.05;

        double totalWidth = totalElapsedTime * scaleFactor;
        ganttChartPane.setPrefWidth(Math.max(500, totalWidth + 50));
        final double currentPaneWidth = ganttChartPane.getPrefWidth();

        // --- Draw Gantt Bars (Rectangles) ---
        for (ScheduleInterval si : scheduleIntervals) {
            double startX = si.start * scaleFactor;
            double width = si.duration * scaleFactor;

            if (width < 0.5) continue;

            Rectangle rect = new Rectangle(startX, 0, width, height);
            rect.setStroke(Color.web("#1a1a2e")); // Dark stroke for definition
            rect.setArcWidth(4);
            rect.setArcHeight(4);

            Color fillColor = Color.web("#3b4a60"); // Soft gray for idle
            String labelText = "IDLE";
            Color labelFill = Color.web("#e8e8e8"); // Light text for dark idle bar

            if (si.process != null) {
                Integer pid = si.process.getPid();
                labelText = String.valueOf(pid);

                fillColor = processColors.getOrDefault(pid, Color.GRAY);
                labelFill = Color.web("#1a1a2e"); // Dark text for light/bright process bar
            }

            rect.setFill(fillColor);
            ganttChartPane.getChildren().add(rect);

            // Add PID/IDLE label
            if (width > 15) {
                Text label = new Text(startX + 5, height - 12, labelText);
                label.setFont(Font.font("Segoe UI", 12));
                label.setFill(labelFill);
                ganttChartPane.getChildren().add(label);
            }
        }

        // --- Draw Time Axis (Below the bars) ---
        final double axisY = height + 25; // Adjusted Y for better spacing

        Line axis = new Line(0, axisY, currentPaneWidth, axisY);
        axis.setStroke(Color.web("#e8e8e8")); // Light axis for dark background
        ganttChartPane.getChildren().add(axis);

        long tickIntervalMs = currentUnit == TimeUnit.SECONDS ? SECOND_IN_MS : 500;
        if (totalElapsedTime > 10000 && currentUnit == TimeUnit.SECONDS) tickIntervalMs = 5000; // Larger intervals for long schedules

        for (long timeMs = 0; timeMs <= totalElapsedTime + tickIntervalMs; timeMs += tickIntervalMs) {
            double x = timeMs * scaleFactor;

            Line tick = new Line(x, axisY - 5, x, axisY + 5);
            tick.setStroke(Color.web("#e8e8e8"));
            ganttChartPane.getChildren().add(tick);

            String labelText = formatTime(timeMs);
            Text timeLabel = new Text(x - 5, axisY + 25, labelText);
            timeLabel.setFill(Color.web("#e8e8e8")); // Light time label
            ganttChartPane.getChildren().add(timeLabel);

            if (x > totalWidth + 10) break;
        }
    }


    // --- Utility Method ---
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    // --- Internal Classes ---
    private static class ScheduleInterval {
        SProcess process;
        long start, duration;

        ScheduleInterval(SProcess p, long s, long d) {
            process = p;
            start = s;
            duration = d;
        }
    }

    // --- PLACEHOLDER SProcess CLASS (Required for controller to compile) ---
    public static class SProcess {
        public enum State { READY, RUNNING, TERMINATED }

        private final int pid;
        private final SimpleStringProperty name;
        private final SimpleLongProperty arrivalTime;
        private long burstTime;
        private long cpuTime;
        private State state;
        private long startTime = -1;
        private long responseTime = -1;
        private long turnaroundTime = -1;
        private long completionTime = -1;
        private long waitingTime = 0;

        public SProcess(int pid, String name, long arrivalTime) {
            this.pid = pid;
            this.name = new SimpleStringProperty(name);
            this.arrivalTime = new SimpleLongProperty(arrivalTime);
            this.cpuTime = 0;
            this.state = State.READY;
        }

        // Getters and Setters
        public int getPid() { return pid; }
        public String getName() { return name.get(); }
        public SimpleStringProperty nameProperty() { return name; }
        public State getState() { return state; }
        public void setState(State state) { this.state = state; }

        public long getBurstTime() { return burstTime; }
        public void setBurstTime(long burstTime) { this.burstTime = burstTime; }
        public long getCpuTime() { return cpuTime; }
        public void setCpuTime(long cpuTime) { this.cpuTime = cpuTime; }
        public long getArrivalTime() { return arrivalTime.get(); }
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        public long getResponseTime() { return responseTime; }
        public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
        public long getTurnaroundTime() { return turnaroundTime; }
        public void setTurnaroundTime(long turnaroundTime) { this.turnaroundTime = turnaroundTime; }
        public long getCompletionTime() { return completionTime; }
        public void setCompletionTime(long completionTime) { this.completionTime = completionTime; }
        public long getWaitingTime() { return waitingTime; }
        public void setWaitingTime(long waitingTime) { this.waitingTime = waitingTime; }
    }
}