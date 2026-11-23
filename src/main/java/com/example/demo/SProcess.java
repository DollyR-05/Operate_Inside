package com.example.demo;

import javafx.beans.property.*;

public class SProcess {

    public enum State { NEW, READY, RUNNING, TERMINATED }

    private final int pid;
    private final StringProperty name;
    private final LongProperty arrivalTime;
    private final LongProperty burstTime;
    private final LongProperty cpuTime;
    private final ObjectProperty<State> state;
    private long startTime = -1;
    private long completionTime = -1;
    private long responseTime = -1;
    private long turnaroundTime = -1;
    private long waitingTime = -1;

    public SProcess(int pid, String name, long arrivalTime) {
        this.pid = pid;
        this.name = new SimpleStringProperty(name);
        this.arrivalTime = new SimpleLongProperty(arrivalTime);
        this.burstTime = new SimpleLongProperty(0);
        this.cpuTime = new SimpleLongProperty(0);
        this.state = new SimpleObjectProperty<>(State.NEW);
    }

    public int getPid() { return pid; }
    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public long getArrivalTime() { return arrivalTime.get(); }
    public LongProperty arrivalTimeProperty() { return arrivalTime; }

    public long getBurstTime() { return burstTime.get(); }
    public void setBurstTime(long bt) { this.burstTime.set(bt); }
    public LongProperty burstTimeProperty() { return burstTime; }

    public long getCpuTime() { return cpuTime.get(); }
    public void setCpuTime(long cpu) { this.cpuTime.set(cpu); }
    public LongProperty cpuTimeProperty() { return cpuTime; }

    public State getState() { return state.get(); }
    public void setState(State s) { this.state.set(s); }
    public ObjectProperty<State> stateProperty() { return state; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long t) { this.startTime = t; }

    public long getCompletionTime() { return completionTime; }
    public void setCompletionTime(long t) { this.completionTime = t; }

    public long getResponseTime() { return responseTime; }
    public void setResponseTime(long t) { this.responseTime = t; }

    public long getTurnaroundTime() { return turnaroundTime; }
    public void setTurnaroundTime(long t) { this.turnaroundTime = t; }

    public long getWaitingTime() { return waitingTime; }
    public void setWaitingTime(long t) { this.waitingTime = t; }

    // String properties for TableView binding, display "-" if value is not set
    public String getResponseTimeString() {
        return responseTime < 0 ? "-" : String.valueOf(responseTime);
    }

    public String getTurnaroundTimeString() {
        return turnaroundTime < 0 ? "-" : String.valueOf(turnaroundTime);
    }

    public String getCompletionTimeString() {
        return completionTime < 0 ? "-" : String.valueOf(completionTime);
    }

    public String getWaitingTimeString() {
        return waitingTime < 0 ? "-" : String.valueOf(waitingTime);
    }
}
