package com.example.demo;

import javafx.beans.property.*;

public class Process {

    public enum State {
        NEW, READY, RUNNING, WAITING, TERMINATED
    }

    private final IntegerProperty pid = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty priority = new SimpleIntegerProperty();
    private final ObjectProperty<State> state = new SimpleObjectProperty<>();
    private final LongProperty cpuTime = new SimpleLongProperty();

    private Thread thread; // To simulate process execution (optional)

    public Process(int pid, String name, int priority) {
        this.pid.set(pid);
        this.name.set(name);
        this.priority.set(priority);
        this.state.set(State.NEW);
        this.cpuTime.set(0L);
    }

    public IntegerProperty pidProperty() { return pid; }
    public StringProperty nameProperty() { return name; }
    public IntegerProperty priorityProperty() { return priority; }
    public ObjectProperty<State> stateProperty() { return state; }
    public LongProperty cpuTimeProperty() { return cpuTime; }

    public int getPid() { return pid.get(); }
    public String getName() { return name.get(); }
    public int getPriority() { return priority.get(); }
    public State getState() { return state.get(); }
    public void setState(State s) { state.set(s); }
    public long getCpuTime() { return cpuTime.get(); }
    public void setCpuTime(long ms) { cpuTime.set(ms); }
    public void addCpuTime(long ms) { setCpuTime(getCpuTime() + ms); }

    public Thread getThread() { return thread; }
    public void setThread(Thread thread) { this.thread = thread; }
}
