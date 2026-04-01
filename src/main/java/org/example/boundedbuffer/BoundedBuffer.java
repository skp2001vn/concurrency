package org.example.boundedbuffer;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe bounded buffer for producer-consumer coordination where
 * producers block when the buffer is full and consumers block when it is
 * empty, using an explicit lock and condition variables.
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
            System.out.println("Produced: " + item);
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
            System.out.println("Consumed: " + item);
            notFull.signal();

            return item;
        } finally {
            lock.unlock();
        }
    }
}
