package org.example.AsyncJobQueue;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * A concurrent job queue that processes tasks asynchronously with a fixed
 * worker pool, retries failed jobs up to a limit, and stores exhausted jobs
 * in a dead-letter queue using explicit lock-based coordination.
 */
public class AsyncJobQueue {

    private final Queue<Job> jobQueue = new LinkedList<>();
    private final Queue<Job> deadLetterQueue = new LinkedList<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    private final ExecutorService workers;
    private final int maxRetries;
    private volatile boolean running = true;

    public AsyncJobQueue(int workerCount, int maxRetries) {
        this.maxRetries = maxRetries;
        workers = Executors.newFixedThreadPool(workerCount);

        for (int i = 0; i < workerCount; i++) {
            workers.submit(this::workerLoop);
        }
    }

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
                    System.out.println("Job success: " + job.getId());
                } catch (Exception e) {
                    job.incrementRetry();
                    if (job.getRetries() <= maxRetries) {
                        System.out.println("Retry job: " + job.getId());
                        submit(job);
                    } else {
                        System.out.println("Job moved to DLQ: " + job.getId());
                        moveToDLQ(job);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public List<Job> getDeadLetterQueue() {
        lock.lock();
        try {
            return new ArrayList<>(deadLetterQueue);
        } finally {
            lock.unlock();
        }
    }

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
