package org.example.simplecyclicbarrier;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A reusable synchronization barrier that blocks threads until a fixed number
 * of parties have called {@code await()}, then releases them together and
 * resets for the next generation.
 */
public class SimpleCyclicBarrier {

    private final int parties;
    private int count = 0;
    private int generation = 0;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition trip = lock.newCondition();

    /**
     * Creates a reusable barrier for the given number of parties.
     *
     * @param parties the number of threads required to trip the barrier
     */
    public SimpleCyclicBarrier(int parties) {
        this.parties = parties;
    }

    /**
     * Waits until the required number of threads have arrived at the barrier.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void await() throws InterruptedException {
        lock.lock();
        try {
            int currentGeneration = generation;
            count++;

            if (count == parties) {
                generation++;
                count = 0;

                trip.signalAll();
                return;
            }

            while (currentGeneration == generation) {
                trip.await();
            }
        } finally {
            lock.unlock();
        }
    }
}
