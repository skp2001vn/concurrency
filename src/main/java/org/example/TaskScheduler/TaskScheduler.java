package org.example.TaskScheduler;

import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A single-worker task scheduler that runs submitted tasks after a delay,
 * ordering them by execution time with a priority queue and coordinating
 * waiting and wakeups with an explicit lock and condition.
 */
public class TaskScheduler {

    private static class ScheduledTask {
        long executionTime;
        Runnable task;

        ScheduledTask(long executionTime, Runnable task) {
            this.executionTime = executionTime;
            this.task = task;
        }
    }

    private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<>(
            (a, b) -> Long.compare(a.executionTime, b.executionTime)
    );

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition newTaskArrived = lock.newCondition();

    private final Thread worker;
    private volatile boolean running = true;

    public TaskScheduler() {
        worker = new Thread(this::runWorker);
        worker.start();
    }

    public void schedule(Runnable task, long delayMillis) {
        long executionTime = System.currentTimeMillis() + delayMillis;

        lock.lock();
        try {
            if (!running) {
                throw new IllegalStateException("Scheduler is shut down");
            }
            queue.offer(new ScheduledTask(executionTime, task));
            newTaskArrived.signal();
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        running = false;
        worker.interrupt();
        lock.lock();
        try {
            newTaskArrived.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void runWorker() {
        while (running) {
            lock.lock();
            try {
                while (queue.isEmpty() && running) {
                    newTaskArrived.await();
                }
                if (!running) {
                    return;
                }

                ScheduledTask nextTask = queue.peek();

                long now = System.currentTimeMillis();
                long waitTime = nextTask.executionTime - now;
                if (waitTime > 0) {
                    newTaskArrived.awaitNanos(waitTime * 1_000_000);
                    continue;
                }

                queue.poll();
                nextTask.task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!running) {
                    return;
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
