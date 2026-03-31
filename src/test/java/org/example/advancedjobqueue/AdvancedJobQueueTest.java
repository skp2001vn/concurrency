package org.example.advancedjobqueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AdvancedJobQueueTest {

    private AdvancedJobQueue queue;

    @AfterEach
    void tearDown() {
        if (queue != null) {
            queue.shutdown();
        }
    }

    @Test
    void executesSubmittedJob() throws InterruptedException {
        queue = new AdvancedJobQueue(1, 0, 25);
        CountDownLatch ran = new CountDownLatch(1);

        queue.submit(new Job("job-success", ran::countDown, 1, 0));

        assertTrue(ran.await(1, TimeUnit.SECONDS));
    }

    @Test
    void cancelledJobDoesNotRun() throws InterruptedException {
        queue = new AdvancedJobQueue(1, 0, 25);
        AtomicBoolean ran = new AtomicBoolean(false);

        queue.submit(new Job("job-cancelled", () -> ran.set(true), 1, 200));
        queue.cancel("job-cancelled");

        Thread.sleep(400);

        assertFalse(ran.get());
    }

    @Test
    void retriesFailedJobBeforeSucceeding() throws InterruptedException {
        queue = new AdvancedJobQueue(1, 2, 25);
        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch success = new CountDownLatch(1);

        queue.submit(new Job("job-retry", () -> {
            if (attempts.incrementAndGet() < 2) {
                throw new RuntimeException("fail once");
            }
            success.countDown();
        }, 1, 0));

        assertTrue(success.await(1, TimeUnit.SECONDS));
        assertEquals(2, attempts.get());
        assertTrue(queue.getDeadLetterQueue().isEmpty());
    }

    @Test
    void movesJobToDeadLetterQueueAfterExhaustingRetries() throws InterruptedException {
        queue = new AdvancedJobQueue(1, 1, 25);

        queue.submit(new Job("job-dlq", () -> {
            throw new RuntimeException("always fails");
        }, 1, 0));

        Job failedJob = queue.getDeadLetterQueue().poll(1, TimeUnit.SECONDS);

        assertNotNull(failedJob);
        assertEquals("job-dlq", failedJob.id);
        assertEquals(2, failedJob.retries);
    }
}
