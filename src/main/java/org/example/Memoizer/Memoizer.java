package org.example.Memoizer;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * a service where multiple threads may request the same expensive computation.
 * @param <K>
 * @param <V>
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