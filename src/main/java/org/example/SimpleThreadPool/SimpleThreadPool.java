package org.example.SimpleThreadPool;

import java.util.*;
import java.util.concurrent.locks.*;


/**
 * A simple fixed-size thread pool that queues submitted tasks and has worker
 * threads wait for and execute them using an explicit lock and condition.
 */

public class SimpleThreadPool {

    private final Queue<Runnable> taskQueue = new LinkedList<>();
    private final List<Worker> workers = new ArrayList<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private volatile boolean running = true;

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
            if (!running) {
                throw new IllegalStateException("Thread pool is shut down");
            }
            taskQueue.offer(task);
            notEmpty.signal();
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

        for (Worker worker : workers) {
            worker.interrupt();
        }
    }

    private class Worker extends Thread {

        @Override
        public void run() {
            while (running) {
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
