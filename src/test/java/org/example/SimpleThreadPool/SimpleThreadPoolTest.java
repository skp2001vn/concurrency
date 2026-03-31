package org.example.SimpleThreadPool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Test
    void reusesFixedNumberOfWorkers() throws InterruptedException {
        pool = new SimpleThreadPool(2);
        CountDownLatch started = new CountDownLatch(4);
        CountDownLatch release = new CountDownLatch(1);
        Set<String> workerNames = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < 4; i++) {
            pool.submit(() -> {
                workerNames.add(Thread.currentThread().getName());
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(150);
        assertEquals(2, workerNames.size());

        release.countDown();
        assertTrue(started.await(1, TimeUnit.SECONDS));
    }

    @Test
    void rejectsNewTasksAfterShutdown() {
        pool = new SimpleThreadPool(1);
        pool.shutdown();

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> pool.submit(() -> {}));

        assertEquals("Thread pool is shut down", thrown.getMessage());
    }
}
