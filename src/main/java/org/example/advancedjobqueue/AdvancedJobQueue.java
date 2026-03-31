package org.example.advancedjobqueue;

import java.util.concurrent.*;

/**
 * A concurrent job queue that executes tasks by scheduled time and priority,
 * supports cancellation, retries failed jobs with exponential backoff, and
 * moves jobs that exceed the retry limit to a dead-letter queue.
 */
public class AdvancedJobQueue {

    private final PriorityBlockingQueue<Job> queue = new PriorityBlockingQueue<>();
    private final BlockingQueue<Job> deadLetterQueue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<String, Job> jobRegistry = new ConcurrentHashMap<>();

    private final ExecutorService workers;
    private final int maxRetries;
    private final long retryBaseDelayMillis;

    public AdvancedJobQueue(int workerCount, int maxRetries) {
        this(workerCount, maxRetries, 1000);
    }

    AdvancedJobQueue(int workerCount, int maxRetries, long retryBaseDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryBaseDelayMillis = retryBaseDelayMillis;

        workers = Executors.newFixedThreadPool(workerCount);
        for (int i = 0; i < workerCount; i++) {
            workers.submit(this::workerLoop);
        }
    }

    public void submit(Job job) {
        jobRegistry.put(job.id, job);
        queue.offer(job);
    }

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

                long now = System.currentTimeMillis();
                if (job.executeAt > now) {
                    queue.offer(job);
                    Thread.sleep(job.executeAt - now);
                    continue;
                }

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

    public BlockingQueue<Job> getDeadLetterQueue() {return deadLetterQueue;}
    public void shutdown() {workers.shutdownNow();}
}
