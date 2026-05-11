package org.example.asyncjobqueue;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * Business logic: processes background jobs asynchronously, retries failed jobs,
 * and preserves exhausted jobs in a dead-letter queue for inspection.
 *
 * <p>Technique: runs a fixed worker pool over a guarded FIFO queue because jobs
 * should be processed asynchronously without creating unbounded threads. A
 * {@link Condition} lets idle workers sleep, and a {@link ReentrantLock} keeps
 * queue, retry, and dead-letter transitions consistent.
 */
public class AsyncJobQueue {

    private final Queue<Job> jobQueue = new LinkedList<>();
    private final Queue<Job> deadLetterQueue = new LinkedList<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    private final ExecutorService workers;
    private final int maxRetries;
    private volatile boolean running = true;

    /**
     * Creates an asynchronous job queue with the given worker count and retry limit.
     *
     * @param workerCount the number of worker threads
     * @param maxRetries the maximum number of retry attempts before dead-lettering a job
     */
    public AsyncJobQueue(int workerCount, int maxRetries) {
        this.maxRetries = maxRetries;
        workers = Executors.newFixedThreadPool(workerCount);

        for (int i = 0; i < workerCount; i++) {
            workers.submit(this::workerLoop);
        }
    }

    /**
     * Enqueues a job for asynchronous processing.
     *
     * @param job the job to process
     */
    public void submit(Job job) {
        lock.lock();
        try {
            jobQueue.offer(job);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    private Job takeJob() throws InterruptedException {
        lock.lock();
        try {
            while (jobQueue.isEmpty() && running) {
                notEmpty.await();
            }
            return jobQueue.poll();
        } finally {
            lock.unlock();
        }
    }

    private void moveToDLQ(Job job) {
        lock.lock();
        try {
            deadLetterQueue.offer(job);
        } finally {
            lock.unlock();
        }
    }

    private void workerLoop() {
        while (running) {
            try {
                Job job = takeJob();
                if (job == null)
                    continue;

                try {
                    job.getTask().run();
                } catch (Exception e) {
                    job.incrementRetry();
                    if (job.getRetries() <= maxRetries) {
                        submit(job);
                    } else {
                        moveToDLQ(job);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Returns a snapshot of the dead-letter queue.
     *
     * @return the jobs that exhausted their retries
     */
    public List<Job> getDeadLetterQueue() {
        lock.lock();
        try {
            return new ArrayList<>(deadLetterQueue);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stops workers and wakes any threads waiting for new jobs.
     */
    public void shutdown() {
        running = false;

        lock.lock();
        try {
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }

        workers.shutdownNow();
    }
}
