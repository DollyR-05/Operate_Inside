package com.example.demo;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessManager {

    private final ObservableList<Process> processList = FXCollections.observableArrayList();
    private final Queue<Process> readyQueue = new LinkedList<>();
    private final AtomicInteger pidGenerator = new AtomicInteger(1);
    private final long TIME_SLICE_MS = 500; // 500 ms time slice

    private volatile boolean isRunning = false;
    private Thread schedulerThread;
    private Process currentProcess = null;

    public ObservableList<Process> getProcessList() {
        return processList;
    }

    // Start the scheduler loop
    public void start() {
        if (isRunning) return; // prevent multiple starts
        isRunning = true;
        schedulerThread = new Thread(() -> {
            while (isRunning) {
                try {
                    if (currentProcess == null) {
                        synchronized (readyQueue) {
                            currentProcess = readyQueue.poll();
                        }
                        if (currentProcess != null) {
                            runProcess(currentProcess);
                        } else {
                            Thread.sleep(100);
                        }
                    } else {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "SchedulerThread");
        schedulerThread.setDaemon(true);
        schedulerThread.start();
    }

    public void stop() {
        isRunning = false;
        if (schedulerThread != null) {
            schedulerThread.interrupt();
        }
    }

    // Add a process with a unique PID
    public void addProcess(String name, int priority) {
        int pid = pidGenerator.getAndIncrement();
        Process p = new Process(pid, name, priority);
        p.setState(Process.State.READY);
        Platform.runLater(() -> {
            processList.add(p);
            synchronized (readyQueue) {
                readyQueue.offer(p);
            }
        });
    }

    // Run process for one time slice
    private void runProcess(Process process) {
        process.setState(Process.State.RUNNING);
        long currentCpuTime = process.getCpuTime();

        try {
            Thread.sleep(TIME_SLICE_MS);
        } catch (InterruptedException ignored) {}

        long newCpuTime = currentCpuTime + TIME_SLICE_MS;
        process.setCpuTime(newCpuTime);

        Platform.runLater(() -> {
            if (process.getState() == Process.State.RUNNING) {
                // Preempt: Set back to READY and add to queue
                process.setState(Process.State.READY);
                synchronized (readyQueue) {
                    readyQueue.offer(process);
                }
            }
            currentProcess = null;
        });
    }
}
