package org.example.SimpleThreadPool;

import java.util.*;
import java.util.concurrent.locks.*;


/**
 * This mimics a simplified version of Java’s ExecutorService.
 * Worker threads continuously pick tasks from a queue.
 */

public class SimpleThreadPool {

    private final Queue<Runnable> taskQueue = new LinkedList<>();
    private final List<Worker> workers = new ArrayList<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public SimpleThreadPool(int numThreads) {
        for (int i = 0; i < numThreads; i++) {
            Worker worker = new Worker();
            workers.add(worker);
            worker.start();
        }
    }

    public void submit(Runnable task) {
        lock.lock();
        try {
            taskQueue.offer(task);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    private class Worker extends Thread {

        public void run() {
            while (true) {
                Runnable task;

                lock.lock();
                try {
                    while (taskQueue.isEmpty()) {
                        notEmpty.await();
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