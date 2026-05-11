package org.example.memoizer;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Business logic: caches expensive values by key so repeated callers can reuse
 * completed work instead of recomputing it.
 *
 * <p>Technique: stores in-progress and completed computations as {@link Future}
 * instances because callers may arrive while a value is still being computed.
 * Guarding insertion with a {@link ReentrantLock} ensures only one
 * {@link FutureTask} is created per key, preventing duplicate expensive work.
 *
 * @param <K> the key type
 * @param <V> the computed value type
 */
public class Memoizer<K, V> {

    private final Map<K, Future<V>> cache = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates an empty memoization cache.
     */
    public Memoizer() {
    }

    /**
     * Computes or returns the cached value for a key, ensuring concurrent callers share the same
     * in-flight computation.
     *
     * @param key the cache key
     * @param task the computation to perform if the key is not yet cached
     * @return the computed value
     * @throws Exception if the computation fails or the waiting thread is interrupted
     */
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
