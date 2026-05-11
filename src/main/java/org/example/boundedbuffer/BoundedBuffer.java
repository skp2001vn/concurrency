package org.example.boundedbuffer;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Business logic: models a bounded handoff queue where producer threads add
 * work items and consumer threads remove them without exceeding fixed capacity.
 *
 * <p>Technique: protects the queue with a {@link ReentrantLock} and uses
 * separate {@link Condition}s because producers and consumers wait for different
 * state changes. This avoids busy-waiting and wakes only the side that can make
 * progress.
 *
 * @param <T> the type of items stored in the buffer
 */
public class BoundedBuffer<T> {

    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    /**
     * Creates a bounded buffer with the given capacity.
     *
     * @param capacity the maximum number of items the buffer can hold
     */
    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Inserts an item into the buffer, blocking while the buffer is full.
     *
     * @param item the item to add
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void produce(T item) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                notFull.await();
            }

            queue.offer(item);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes and returns the next item from the buffer, blocking while the buffer is empty.
     *
     * @return the next available item
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public T consume() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }

            T item = queue.poll();
            notFull.signal();

            return item;
        } finally {
            lock.unlock();
        }
    }
}
