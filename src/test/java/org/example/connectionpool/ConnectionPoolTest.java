package org.example.connectionpool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ConnectionPoolTest {

    @Test
    void acquiresAndReleasesConnection() throws InterruptedException {
        ConnectionPool pool = new ConnectionPool(1, 1);

        Connection first = pool.acquire(100);
        assertNotNull(first);

        pool.release(first);

        Connection second = pool.acquire(100);
        assertNotNull(second);
        assertEquals(first.getId(), second.getId());
    }

    @Test
    void returnsNullWhenAcquireTimesOut() throws InterruptedException {
        ConnectionPool pool = new ConnectionPool(1, 1);
        Connection held = pool.acquire(100);
        assertNotNull(held);

        Connection timedOut = pool.acquire(150);

        assertNull(timedOut);
        pool.release(held);
    }

    @Test
    void waitingAcquireSucceedsAfterConnectionIsReleased() throws InterruptedException {
        ConnectionPool pool = new ConnectionPool(1, 1);
        Connection held = pool.acquire(100);
        assertNotNull(held);

        CountDownLatch waiting = new CountDownLatch(1);
        AtomicReference<Connection> acquired = new AtomicReference<>();

        Thread waiter = new Thread(() -> {
            waiting.countDown();
            try {
                acquired.set(pool.acquire(1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        waiter.start();

        assertTrue(waiting.await(1, TimeUnit.SECONDS));
        Thread.sleep(100);
        pool.release(held);
        waiter.join(1000);

        assertNotNull(acquired.get());
        assertEquals(held.getId(), acquired.get().getId());
    }

    @Test
    void rejectsAcquireWhenTooManyThreadsAreAlreadyWaiting() throws InterruptedException {
        ConnectionPool pool = new ConnectionPool(1, 1);
        Connection held = pool.acquire(100);
        assertNotNull(held);

        CountDownLatch waiting = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread firstWaiter = new Thread(() -> {
            waiting.countDown();
            try {
                pool.acquire(500);
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        firstWaiter.start();

        assertTrue(waiting.await(1, TimeUnit.SECONDS));
        Thread.sleep(100);

        RuntimeException thrown = null;
        try {
            pool.acquire(100);
        } catch (RuntimeException e) {
            thrown = e;
        }

        assertNotNull(thrown);
        assertEquals("Too many waiting threads", thrown.getMessage());

        pool.release(held);
        firstWaiter.join(1000);
    }
}
