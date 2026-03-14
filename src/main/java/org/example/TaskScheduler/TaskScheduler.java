package org.example.TaskScheduler;

import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 	scheduling tasks to run after a delay
 * 	thread coordination
 * 	PriorityQueue for ordering tasks
 * 	ReentrantLock + Condition for waiting and signaling
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
            (a, b) -> Long.compare(b.executionTime, a.executionTime)
    );

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition newTaskArrived = lock.newCondition();

    private final Thread worker;

    public TaskScheduler() {
        worker = new Thread(this::runWorker);
        worker.start();
    }

    public void schedule(Runnable task, long delayMillis) {
        long executionTime = System.currentTimeMillis() + delayMillis;

        lock.lock();
        try {
            queue.offer(new ScheduledTask(executionTime, task));
            newTaskArrived.signal();
        } finally {
            lock.unlock();
        }
    }

    private void runWorker() {
        while (true) {
            lock.lock();
            try {
                while (queue.isEmpty()) {
                    newTaskArrived.await();
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
            } finally {
                lock.unlock();
            }
        }
    }
}