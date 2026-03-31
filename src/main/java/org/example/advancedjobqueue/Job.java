package org.example.advancedjobqueue;

import java.util.concurrent.atomic.AtomicBoolean;

class Job implements Comparable<Job> {

    String id;
    Runnable task;

    int priority;
    int retries;

    long executeAt;

    AtomicBoolean cancelled = new AtomicBoolean(false);

    public Job(String id, Runnable task, int priority, long delayMillis) {
        this.id = id;
        this.task = task;
        this.priority = priority;

        this.executeAt = System.currentTimeMillis() + delayMillis;
    }

    @Override
    public int compareTo(Job other) {
        if (this.executeAt != other.executeAt)
            return Long.compare(this.executeAt, other.executeAt);

        return Integer.compare(other.priority, this.priority);
    }
}