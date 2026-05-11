package org.example.simplereadwritelock;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Business logic: protects shared state whose reads can safely run together but
 * whose writes must run exclusively.
 *
 * <p>Technique: tracks active readers, active writers, and waiting writers under
 * a {@link ReentrantLock} because read and write admission depend on shared
 * counters. Separate {@link Condition}s allow targeted wakeups, and writer
 * preference reduces starvation when writes are waiting.
 */
public class SimpleReadWriteLock {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition canRead = lock.newCondition();
    private final Condition canWrite = lock.newCondition();

    private int activeReaders = 0;
    private int activeWriters = 0;
    private int waitingWriters = 0;

    /**
     * Creates an unlocked reader-writer lock.
     */
    public SimpleReadWriteLock() {
    }

    /**
     * Acquires the read lock, waiting while a writer is active or queued.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void lockRead() throws InterruptedException {
        lock.lock();
        try {
            while (activeWriters > 0 || waitingWriters > 0) {
                canRead.await();
            }

            activeReaders++;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Releases the read lock.
     */
    public void unlockRead() {
        lock.lock();
        try {
            activeReaders--;
            if (activeReaders == 0) {
                canWrite.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Acquires the write lock, waiting until no readers or writers remain active.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void lockWrite() throws InterruptedException {
        lock.lock();
        try {
            waitingWriters++;
            while (activeReaders > 0 || activeWriters > 0) {
                canWrite.await();
            }

            waitingWriters--;
            activeWriters++;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Releases the write lock.
     */
    public void unlockWrite() {
        lock.lock();
        try {
            activeWriters--;

            if (waitingWriters > 0) {
                canWrite.signal();
            } else {
                canRead.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
}
