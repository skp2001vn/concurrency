package org.example.SimpleThreadPool;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * This mimics a simplified version of Java’s ExecutorService.
 * Worker threads continuously pick tasks from a queue.
 */
public class SimpleThreadPool {

    private final Queue<Runnable> taskQueue = new LinkedList<>();
    private final List<Worker> workers = new LinkedList<>();

    public SimpleThreadPool(int numThreads) {

        for (int i = 0; i < numThreads; i++) {
            Worker worker = new Worker();
            workers.add(worker);
            worker.start();
        }
    }

    public void submit(Runnable task) {
        synchronized (taskQueue) {
            taskQueue.offer(task);
            taskQueue.notify();
        }
    }

    private class Worker extends Thread {

        public void run() {
            while (true) {
                Runnable task;
                synchronized (taskQueue) {
                    while (taskQueue.isEmpty()) {
                        try {
                            taskQueue.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    task = taskQueue.poll();
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