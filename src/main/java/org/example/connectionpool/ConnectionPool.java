package org.example.connectionpool;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

/**
 * Business logic: shares a finite set of reusable connections among callers
 * while bounding how long and how many callers may wait.
 *
 * <p>Technique: uses a fair {@link ReentrantLock} because acquisition order
 * matters when many callers compete for scarce connections. A {@link Condition}
 * enables timed waiting without polling, and the waiter limit prevents unbounded
 * contention under load.
 */
public class ConnectionPool {

    private final Queue<Connection> pool;

    private final ReentrantLock lock = new ReentrantLock(true); // fair lock
    private final Condition notEmpty = lock.newCondition();

    private int waitingThreads = 0;
    private final int maxWaitingThreads;

    /**
     * Creates a connection pool with the given size and maximum waiter count.
     *
     * @param size the number of reusable connections in the pool
     * @param maxWaitingThreads the maximum number of threads allowed to wait for a connection
     */
    public ConnectionPool(int size, int maxWaitingThreads) {

        pool = new LinkedList<>();
        this.maxWaitingThreads = maxWaitingThreads;

        for (int i = 0; i < size; i++) {
            pool.offer(new Connection(i));
        }
    }

    /**
     * Acquires a connection, waiting up to the provided timeout.
     *
     * @param timeoutMillis the maximum time to wait, in milliseconds
     * @return a pooled connection, or {@code null} if the wait timed out
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public Connection acquire(long timeoutMillis) throws InterruptedException {
        long nanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);

        lock.lock();
        try {
            if (waitingThreads >= maxWaitingThreads) {
                throw new RuntimeException("Too many waiting threads");
            }
            waitingThreads++;
            try {
                while (pool.isEmpty()) {
                    if (nanos <= 0) {
                        return null;
                    }
                    nanos = notEmpty.awaitNanos(nanos);
                }
                return pool.poll();
            } finally {
                waitingThreads--;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a connection to the pool and wakes one waiting acquirer.
     *
     * @param conn the connection to release back to the pool
     */
    public void release(Connection conn) {
        lock.lock();
        try {
            pool.offer(conn);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }
}
