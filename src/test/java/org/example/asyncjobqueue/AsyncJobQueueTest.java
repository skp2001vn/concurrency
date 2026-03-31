package org.example.asyncjobqueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AsyncJobQueueTest {

    private AsyncJobQueue queue;

    @AfterEach
    void tearDown() {
        if (queue != null) {
            queue.shutdown();
        }
    }

    @Test
    void executesSubmittedJob() throws InterruptedException {
        queue = new AsyncJobQueue(1, 0);
        CountDownLatch ran = new CountDownLatch(1);

        queue.submit(new Job("job-success", ran::countDown));

        assertTrue(ran.await(1, TimeUnit.SECONDS));
    }

    @Test
    void retriesFailedJobBeforeSucceeding() throws InterruptedException {
        queue = new AsyncJobQueue(1, 2);
        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch success = new CountDownLatch(1);

        queue.submit(new Job("job-retry", () -> {
            if (attempts.incrementAndGet() < 2) {
                throw new RuntimeException("fail once");
            }
            success.countDown();
        }));

        assertTrue(success.await(1, TimeUnit.SECONDS));
        assertEquals(2, attempts.get());
        assertTrue(queue.getDeadLetterQueue().isEmpty());
    }

    @Test
    void movesFailedJobToDeadLetterQueueAfterExhaustingRetries() throws InterruptedException {
        queue = new AsyncJobQueue(1, 1);

        queue.submit(new Job("job-dlq", () -> {
            throw new RuntimeException("always fails");
        }));

        assertTrue(awaitCondition(() -> queue.getDeadLetterQueue().size() == 1, 1000));
        Job failedJob = queue.getDeadLetterQueue().get(0);
        assertEquals("job-dlq", failedJob.getId());
        assertEquals(2, failedJob.getRetries());
    }

    @Test
    void shutdownStopsWorkersFromExecutingLaterTasks() throws InterruptedException {
        queue = new AsyncJobQueue(1, 0);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch unblock = new CountDownLatch(1);
        AtomicInteger completed = new AtomicInteger();

        queue.submit(new Job("job-blocking", () -> {
            started.countDown();
            try {
                unblock.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            completed.incrementAndGet();
        }));
        queue.submit(new Job("job-later", completed::incrementAndGet));

        assertTrue(started.await(1, TimeUnit.SECONDS));
        queue.shutdown();
        unblock.countDown();
        Thread.sleep(200);

        assertFalse(awaitCondition(() -> completed.get() > 1, 300));
    }

    private boolean awaitCondition(BooleanSupplier condition, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(10);
        }
        return condition.getAsBoolean();
    }
}
