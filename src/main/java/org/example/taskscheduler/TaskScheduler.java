package org.example.taskscheduler;

import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import lombok.AllArgsConstructor;

/**
 * Business logic: runs submitted tasks after their requested delay, similar to
 * a small single-threaded reminder or deferred-job scheduler.
 *
 * <p>Technique: orders tasks by execution time in a priority queue because the
 * earliest due task must run first. A {@link ReentrantLock} and
 * {@link Condition} let the worker sleep until either a new earlier task arrives
 * or the current head task becomes due.
 */
public class TaskScheduler {

    @AllArgsConstructor
    private static class ScheduledTask {
        long executionTime;
        Runnable task;
    }

    private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<>(
            (a, b) -> Long.compare(a.executionTime, b.executionTime)
    );

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition newTaskArrived = lock.newCondition();

    private final Thread worker;
    private volatile boolean running = true;

    /**
     * Creates and starts the scheduler worker thread.
     */
    public TaskScheduler() {
        worker = new Thread(this::runWorker);
        worker.start();
    }

    /**
     * Schedules a task to run after the given delay.
     *
     * @param task the task to execute
     * @param delayMillis the delay before execution, in milliseconds
     * @throws IllegalStateException if the scheduler has already been shut down
     */
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

    /**
     * Stops the scheduler worker and wakes it if it is currently waiting.
     */
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
            Runnable taskToRun = null;

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
                taskToRun = nextTask.task;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!running) {
                    return;
                }
            } finally {
                lock.unlock();
            }

            try {
                if (taskToRun != null) {
                    taskToRun.run();
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }
}
