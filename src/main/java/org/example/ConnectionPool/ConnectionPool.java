package org.example.ConnectionPool;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

/**
 * Build a thread-safe connection pool that supports timeouts, fairness, and max waiting threads.
 */
public class ConnectionPool {

    private final Queue<Connection> pool;

    private final ReentrantLock lock = new ReentrantLock(true); // fair lock
    private final Condition notEmpty = lock.newCondition();

    private int waitingThreads = 0;
    private final int maxWaitingThreads;

    public ConnectionPool(int size, int maxWaitingThreads) {

        pool = new LinkedList<>();
        this.maxWaitingThreads = maxWaitingThreads;

        for (int i = 0; i < size; i++) {
            pool.offer(new Connection(i));
        }
    }

    public Connection acquire(long timeoutMillis)
            throws InterruptedException {

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