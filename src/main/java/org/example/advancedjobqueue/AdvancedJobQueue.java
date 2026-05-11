package org.example.advancedjobqueue;

import java.util.concurrent.*;

/**
 * Business logic: executes scheduled jobs by due time and priority, supports
 * cancellation before execution, retries failures, and dead-letters exhausted
 * jobs.
 *
 * <p>Technique: uses a {@link DelayQueue} of {@link Job}s because scheduled
 * jobs should become available only when due. A fixed worker pool bounds
 * execution resources, a concurrent registry makes cancellation lookup safe,
 * exponential backoff avoids immediate retry loops, and a
 * {@link LinkedBlockingQueue} preserves failed jobs for inspection.
 */
public class AdvancedJobQueue {

    private final DelayQueue<Job> queue = new DelayQueue<>();
    private final BlockingQueue<Job> deadLetterQueue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<String, Job> jobRegistry = new ConcurrentHashMap<>();

    private final ExecutorService workers;
    private final int maxRetries;
    private final long retryBaseDelayMillis;

    /**
     * Creates a queue with the given worker count and retry limit.
     *
     * @param workerCount the number of worker threads
     * @param maxRetries the maximum number of retry attempts before dead-lettering a job
     */
    public AdvancedJobQueue(int workerCount, int maxRetries) {
        this(workerCount, maxRetries, 1000);
    }

    /**
     * Creates a queue with an explicit retry base delay for deterministic tests.
     *
     * @param workerCount the number of worker threads
     * @param maxRetries the maximum number of retry attempts before dead-lettering a job
     * @param retryBaseDelayMillis the base delay used to calculate exponential retry backoff
     */
    AdvancedJobQueue(int workerCount, int maxRetries, long retryBaseDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryBaseDelayMillis = retryBaseDelayMillis;

        workers = Executors.newFixedThreadPool(workerCount);
        for (int i = 0; i < workerCount; i++) {
            workers.submit(this::workerLoop);
        }
    }

    /**
     * Submits a job for scheduled execution.
     *
     * @param job the job to enqueue
     */
    public void submit(Job job) {
        jobRegistry.put(job.id, job);
        queue.offer(job);
    }

    /**
     * Marks a job as cancelled so workers skip it if it has not already run.
     *
     * @param jobId the identifier of the job to cancel
     */
    public void cancel(String jobId) {
        Job job = jobRegistry.get(jobId);
        if (job != null) {
            job.cancelled.set(true);
        }
    }

    private void workerLoop() {
        while (true) {
            try {
                Job job = queue.take();
                if (job.cancelled.get())
                    continue;

                try {
                    job.task.run();
                    jobRegistry.remove(job.id);
                    System.out.println("Job success: " + job.id);
                } catch (Exception e) {
                    job.retries++;
                    if (job.retries <= maxRetries) {
                        long backoff = (long) Math.pow(2, job.retries) * retryBaseDelayMillis;
                        job.executeAt =
                                System.currentTimeMillis() + backoff;

                        queue.offer(job);
                        System.out.println(
                                "Retry scheduled: " + job.id);
                    } else {
                        deadLetterQueue.offer(job);
                        jobRegistry.remove(job.id);

                        System.out.println(
                                "Moved to DLQ: " + job.id);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Returns the dead-letter queue containing jobs that exhausted retries.
     *
     * @return the queue of failed jobs
     */
    public BlockingQueue<Job> getDeadLetterQueue() {
        return deadLetterQueue;
    }

    /**
     * Stops all worker threads immediately.
     */
    public void shutdown() {
        workers.shutdownNow();
    }
}
