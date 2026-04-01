package org.example.advancedjobqueue;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class Job implements Delayed {

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
    public long getDelay(TimeUnit unit) {
        long delayMillis = executeAt - System.currentTimeMillis();
        return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        Job otherJob = (Job) other;
        if (this.executeAt != otherJob.executeAt)
            return Long.compare(this.executeAt, otherJob.executeAt);

        return Integer.compare(otherJob.priority, this.priority);
    }
}
