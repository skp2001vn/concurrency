package org.example.simplecyclicbarrier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SimpleCyclicBarrierTest {

    @Test
    void waitsUntilRequiredNumberOfThreadsArrive() throws InterruptedException {
        SimpleCyclicBarrier barrier = new SimpleCyclicBarrier(3);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch passed = new CountDownLatch(2);

        Runnable waitingTask = () -> {
            started.countDown();
            try {
                barrier.await();
                passed.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Thread t1 = new Thread(waitingTask);
        Thread t2 = new Thread(waitingTask);
        t1.start();
        t2.start();

        assertTrue(started.await(1, TimeUnit.SECONDS));
        Thread.sleep(100);
        assertEquals(2, passed.getCount());

        Thread releaser = new Thread(() -> {
            try {
                barrier.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        releaser.start();

        assertTrue(passed.await(1, TimeUnit.SECONDS));
        t1.join(1000);
        t2.join(1000);
        releaser.join(1000);
        assertFalse(t1.isAlive());
        assertFalse(t2.isAlive());
        assertFalse(releaser.isAlive());
    }

    @Test
    void canBeReusedAcrossGenerations() throws InterruptedException {
        SimpleCyclicBarrier barrier = new SimpleCyclicBarrier(2);
        List<String> passed = new CopyOnWriteArrayList<>();

        Thread firstA = new Thread(() -> awaitAndRecord(barrier, passed, "g1-a"));
        Thread firstB = new Thread(() -> awaitAndRecord(barrier, passed, "g1-b"));
        firstA.start();
        firstB.start();
        firstA.join(1000);
        firstB.join(1000);

        Thread secondA = new Thread(() -> awaitAndRecord(barrier, passed, "g2-a"));
        Thread secondB = new Thread(() -> awaitAndRecord(barrier, passed, "g2-b"));
        secondA.start();
        secondB.start();
        secondA.join(1000);
        secondB.join(1000);

        assertEquals(4, passed.size());
        assertTrue(passed.contains("g1-a"));
        assertTrue(passed.contains("g1-b"));
        assertTrue(passed.contains("g2-a"));
        assertTrue(passed.contains("g2-b"));
    }

    private void awaitAndRecord(SimpleCyclicBarrier barrier, List<String> passed, String marker) {
        try {
            barrier.await();
            passed.add(marker);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
