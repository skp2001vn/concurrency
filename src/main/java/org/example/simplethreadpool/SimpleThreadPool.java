package org.example.simplethreadpool;

import java.util.*;
import java.util.concurrent.locks.*;


/**
 * Business logic: accepts independent tasks and executes them asynchronously on
 * a fixed set of reusable worker threads.
 *
 * <p>Technique: stores tasks in a guarded queue and wakes workers with a
 * {@link Condition} because workers should sleep when there is no work. A fixed
 * worker set avoids creating a new thread per task. The same lock guards task
 * admission and shutdown so accepted tasks finish before workers exit.
 *
 * <p>This is a learning example; application code should normally use Java's
 * {@link java.util.concurrent.ExecutorService} implementations.
 */
public class SimpleThreadPool {

    private final Queue<Runnable> taskQueue = new LinkedList<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private boolean running = true;

    /**
     * Creates a fixed-size thread pool with the given number of worker threads.
     *
     * @param numThreads the number of worker threads to start
     */
    public SimpleThreadPool(int numThreads) {
        for (int i = 0; i < numThreads; i++) {
            Worker worker = new Worker();
            worker.start();
        }
    }

    /**
     * Submits a task for asynchronous execution.
     *
     * @param task the task to run
     * @throws IllegalStateException if the pool has already been shut down
     */
    public void submit(Runnable task) {
        lock.lock();
        try {
            if (!running) {
                throw new IllegalStateException("Thread pool is shut down");
            }
            taskQueue.offer(task);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stops accepting new tasks and wakes idle workers. Running and queued tasks
     * finish without being interrupted by shutdown, then workers terminate.
     * This method does not wait for termination; repeated calls are harmless.
     */
    public void shutdown() {
        lock.lock();
        try {
            running = false;
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private class Worker extends Thread {

        @Override
        public void run() {
            while (true) {
                Runnable task;

                lock.lock();
                try {
                    while (taskQueue.isEmpty() && running) {
                        notEmpty.await();
                    }
                    if (!running && taskQueue.isEmpty()) {
                        return;
                    }
                    task = taskQueue.poll();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    lock.unlock();
                }

                try {
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
