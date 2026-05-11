package org.example.advancedjobqueue;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Business logic: represents a scheduled job with an identifier, task,
 * priority, retry count, due time, and cancellation state.
 *
 * <p>Technique: implements {@link Delayed} because {@link java.util.concurrent.DelayQueue}
 * can then block workers until the job is due. The comparison also orders jobs
 * by priority when due times match, giving predictable scheduling without a
 * separate timer thread.
 */
class Job implements Delayed {

    String id;
    Runnable task;

    int priority;
    int retries;

    long executeAt;

    AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * Creates a scheduled job.
     *
     * @param id stable identifier used for cancellation and registry tracking
     * @param task work to execute
     * @param priority higher values run first when jobs have the same due time
     * @param delayMillis delay before the job becomes eligible to run
     */
    public Job(String id, Runnable task, int priority, long delayMillis) {
        this.id = id;
        this.task = task;
        this.priority = priority;

        this.executeAt = System.currentTimeMillis() + delayMillis;
    }

    /**
     * Returns the remaining delay before this job is eligible for execution.
     *
     * @param unit the unit to convert the remaining delay into
     * @return the converted remaining delay
     */
    @Override
    public long getDelay(TimeUnit unit) {
        long delayMillis = executeAt - System.currentTimeMillis();
        return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Orders jobs first by due time, then by descending priority.
     *
     * @param other the delayed job to compare against
     * @return a negative value when this job should be taken first
     */
    @Override
    public int compareTo(Delayed other) {
        Job otherJob = (Job) other;
        if (this.executeAt != otherJob.executeAt)
            return Long.compare(this.executeAt, otherJob.executeAt);

        return Integer.compare(otherJob.priority, this.priority);
    }
}
