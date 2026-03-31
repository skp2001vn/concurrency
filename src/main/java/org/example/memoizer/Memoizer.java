package org.example.memoizer;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe memoization cache that stores in-progress and completed
 * computations as futures so concurrent requests for the same key share a
 * single expensive computation.
 *
 * @param <K> the key type
 * @param <V> the computed value type
 */
public class Memoizer<K, V> {

    private final Map<K, Future<V>> cache = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public V compute(K key, Callable<V> task) throws Exception {

        Future<V> future;
        lock.lock();
        try {
            future = cache.get(key);
            if (future == null) {
                FutureTask<V> futureTask = new FutureTask<>(task);
                cache.put(key, futureTask);
                future = futureTask;
                futureTask.run();
            }
        } finally {
            lock.unlock();
        }

        return future.get();
    }
}
