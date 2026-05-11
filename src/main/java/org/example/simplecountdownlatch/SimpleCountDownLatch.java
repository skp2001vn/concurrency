package org.example.simplecountdownlatch;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Business logic: represents a one-shot gate where callers wait until a fixed
 * number of independent completions have been reported.
 *
 * <p>Technique: guards the remaining count with a {@link ReentrantLock} because
 * decrement and wait decisions must be atomic. A single {@link Condition}
 * releases all waiters when the count reaches zero and avoids polling while
 * work is still outstanding.
 */
public class SimpleCountDownLatch {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition zeroReached = lock.newCondition();

    private int count;

    /**
     * Creates a latch with the given initial count.
     *
     * @param count the number of countdowns required before waiters are released
     */
    public SimpleCountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }
        this.count = count;
    }

    /**
     * Waits until the latch count reaches zero.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void await() throws InterruptedException {
        lock.lock();
        try {
            while (count > 0) {
                zeroReached.await();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Decrements the count by one if it is still above zero and releases all
     * waiters when the count reaches zero.
     */
    public void countDown() {
        lock.lock();
        try {
            if (count == 0) {
                return;
            }

            count--;
            if (count == 0) {
                zeroReached.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the current latch count.
     *
     * @return the remaining number of countdowns before the latch opens
     */
    public int getCount() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
