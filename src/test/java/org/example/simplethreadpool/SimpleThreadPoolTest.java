package org.example.simplethreadpool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SimpleThreadPoolTest {

    private SimpleThreadPool pool;

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.shutdown();
        }
    }

    /**
     * Verifies that submitted tasks are executed by the pool.
     */
    @Test
    void executesSubmittedTasks() throws InterruptedException {
        pool = new SimpleThreadPool(2);
        CountDownLatch completed = new CountDownLatch(3);
        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < 3; i++) {
            pool.submit(() -> {
                counter.incrementAndGet();
                completed.countDown();
            });
        }

        assertTrue(completed.await(1, TimeUnit.SECONDS));
        assertEquals(3, counter.get());
    }

    /**
     * Verifies that the pool reuses a fixed worker set instead of creating new threads per task.
     */
    @Test
    void reusesFixedNumberOfWorkers() throws InterruptedException {
        pool = new SimpleThreadPool(2);
        CountDownLatch firstWorkersStarted = new CountDownLatch(2);
        CountDownLatch started = new CountDownLatch(4);
        CountDownLatch release = new CountDownLatch(1);
        Set<String> workerNames = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < 4; i++) {
            pool.submit(() -> {
                workerNames.add(Thread.currentThread().getName());
                firstWorkersStarted.countDown();
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        try {
            assertTrue(firstWorkersStarted.await(5, TimeUnit.SECONDS));
            assertEquals(2, workerNames.size());
        } finally {
            release.countDown();
        }
        assertTrue(started.await(1, TimeUnit.SECONDS));
    }

    /**
     * Verifies that shutdown drains all accepted tasks and tolerates repeated calls.
     */
    @Test
    void shutdownDrainsQueuedTasks() throws InterruptedException {
        pool = new SimpleThreadPool(1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Thread> worker = new AtomicReference<>();
        AtomicInteger completed = new AtomicInteger();

        pool.submit(() -> {
            worker.set(Thread.currentThread());
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
            for (int i = 0; i < 3; i++) {
                pool.submit(completed::incrementAndGet);
            }
            pool.shutdown();
            pool.shutdown();
        } finally {
            release.countDown();
            pool.shutdown();
        }

        worker.get().join(5_000);
        assertFalse(worker.get().isAlive(), "Worker should terminate after draining the queue");
        assertEquals(3, completed.get());
    }

    /**
     * Verifies that shutdown returns without interrupting a task that is still running.
     */
    @Test
    void shutdownDoesNotInterruptRunningTasks() throws InterruptedException {
        pool = new SimpleThreadPool(1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Thread> worker = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean();

        pool.submit(() -> {
            worker.set(Thread.currentThread());
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            }
            interrupted.set(interrupted.get() || Thread.currentThread().isInterrupted());
        });

        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
            pool.shutdown();
        } finally {
            release.countDown();
            pool.shutdown();
        }

        worker.get().join(5_000);
        assertFalse(worker.get().isAlive(), "Worker should terminate after the task finishes");
        assertFalse(interrupted.get(), "Graceful shutdown must not interrupt running tasks");
    }

    /**
     * Verifies that shutdown wakes and terminates every worker waiting on an empty queue.
     */
    @Test
    void shutdownTerminatesIdleWorkers() throws InterruptedException {
        pool = new SimpleThreadPool(2);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch tasksFinished = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Set<Thread> workers = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                workers.add(Thread.currentThread());
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    tasksFinished.countDown();
                }
            });
        }

        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
        } finally {
            release.countDown();
        }
        // Once both tasks have returned, WAITING means the workers are awaiting more work.
        assertTrue(tasksFinished.await(5, TimeUnit.SECONDS));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (workers.stream().anyMatch(worker -> worker.getState() != Thread.State.WAITING)
                && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertTrue(workers.stream().allMatch(worker -> worker.getState() == Thread.State.WAITING));

        pool.shutdown();

        for (Thread worker : workers) {
            worker.join(5_000);
            assertFalse(worker.isAlive(), "Idle worker should terminate after shutdown");
        }
    }

    /**
     * Verifies that task submission is rejected after shutdown.
     */
    @Test
    void rejectsNewTasksAfterShutdown() {
        pool = new SimpleThreadPool(1);
        pool.shutdown();

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> pool.submit(() -> {}));

        assertEquals("Thread pool is shut down", thrown.getMessage());
    }
}
