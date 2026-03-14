package org.example.AsyncJobQueue;

import java.util.concurrent.*;

/**
 * an async job queue with a fixed thread pool, retry logic, and a dead-letter queue for failed jobs.
 */
public class AsyncJobQueue {

    private final BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Job> deadLetterQueue = new LinkedBlockingQueue<>();
    private final ExecutorService workers;

    private final int maxRetries;

    public AsyncJobQueue(int workerCount, int maxRetries) {
        this.maxRetries = maxRetries;
        workers = Executors.newFixedThreadPool(workerCount);

        for (int i = 0; i < workerCount; i++) {
            workers.submit(this::workerLoop);
        }
    }

    public void submit(Job job) {
        jobQueue.offer(job);
    }

    private void workerLoop() {
        while (true) {
            try {
                Job job = jobQueue.take();
                try {
                    job.getTask().run();
                    System.out.println(
                            "Job success: " + job.getId()
                    );
                } catch (Exception e) {
                    job.incrementRetry();
                    if (job.getRetries() <= maxRetries) {
                        System.out.println(
                                "Retry job: " + job.getId()
                        );

                        jobQueue.offer(job);
                    } else {
                        System.out.println(
                                "Job moved to DLQ: " + job.getId()
                        );

                        deadLetterQueue.offer(job);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public BlockingQueue<Job> getDeadLetterQueue() {
        return deadLetterQueue;
    }

    public void shutdown() {
        workers.shutdownNow();
    }
}

