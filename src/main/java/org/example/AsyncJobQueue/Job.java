package org.example.AsyncJobQueue;

public class Job {

    private final String id;
    private final Runnable task;
    private int retries;

    public Job(String id, Runnable task) {
        this.id = id;
        this.task = task;
        this.retries = 0;
    }

    public String getId() {
        return id;
    }

    public Runnable getTask() {
        return task;
    }

    public int getRetries() {
        return retries;
    }

    public void incrementRetry() {
        retries++;
    }

}
