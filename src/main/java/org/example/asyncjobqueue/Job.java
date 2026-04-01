package org.example.asyncjobqueue;

public class Job {

    private final String id;
    private final Runnable task;
    private int retries;

    /**
     * Creates a job with the provided identifier and task.
     *
     * @param id the job identifier
     * @param task the task to execute
     */
    public Job(String id, Runnable task) {
        this.id = id;
        this.task = task;
        this.retries = 0;
    }

    /**
     * Returns the job identifier.
     *
     * @return the job identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the executable task associated with the job.
     *
     * @return the job task
     */
    public Runnable getTask() {
        return task;
    }

    /**
     * Returns the number of retries attempted so far.
     *
     * @return the retry count
     */
    public int getRetries() {
        return retries;
    }

    /**
     * Increments the retry counter after a failed attempt.
     */
    public void incrementRetry() {
        retries++;
    }

}
