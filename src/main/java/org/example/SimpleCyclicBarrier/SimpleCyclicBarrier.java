package org.example.SimpleCyclicBarrier;

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

    public SimpleCyclicBarrier(int parties) {
        this.parties = parties;
    }

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
