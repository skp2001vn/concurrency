package org.example.simplecountdownlatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SimpleCountDownLatchTest {

    @Test
    void waitsUntilCountReachesZero() throws InterruptedException {
        SimpleCountDownLatch latch = new SimpleCountDownLatch(2);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch passed = new CountDownLatch(2);

        Runnable waitingTask = () -> {
            started.countDown();
            try {
                latch.await();
                passed.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Thread first = new Thread(waitingTask);
        Thread second = new Thread(waitingTask);
        first.start();
        second.start();

        assertTrue(started.await(1, TimeUnit.SECONDS));
        assertFalse(passed.await(150, TimeUnit.MILLISECONDS));

        latch.countDown();
        assertFalse(passed.await(150, TimeUnit.MILLISECONDS));
        assertEquals(1, latch.getCount());

        latch.countDown();

        assertTrue(passed.await(1, TimeUnit.SECONDS));
        first.join(1000);
        second.join(1000);
        assertFalse(first.isAlive());
        assertFalse(second.isAlive());
        assertEquals(0, latch.getCount());
    }

    @Test
    void awaitReturnsImmediatelyWhenCountIsZero() throws InterruptedException {
        SimpleCountDownLatch latch = new SimpleCountDownLatch(0);

        assertTimeoutPreemptively(Duration.ofMillis(100), latch::await);

        assertEquals(0, latch.getCount());
    }

    @Test
    void countDownAfterLatchIsOpenHasNoEffect() throws InterruptedException {
        SimpleCountDownLatch latch = new SimpleCountDownLatch(1);

        latch.countDown();
        latch.countDown();
        latch.await();

        assertEquals(0, latch.getCount());
    }

    @Test
    void rejectsNegativeInitialCount() {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> new SimpleCountDownLatch(-1)
        );

        assertEquals("count must be non-negative", thrown.getMessage());
    }
}
