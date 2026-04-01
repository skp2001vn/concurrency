package org.example.expiringcache;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe cache that stores entries with per-item expiration times,
 * removes expired values on access, and periodically cleans them up in the
 * background with a scheduled task.
 */
public class ExpiringCache<K, V> {

    private record CacheEntry<V>(V value, long expireAt) {
    }

    private final Map<K, CacheEntry<V>> cache = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    private final ScheduledExecutorService cleaner =
            Executors.newSingleThreadScheduledExecutor();

    /**
     * Creates a cache with a default cleanup interval of five seconds.
     */
    public ExpiringCache() {
        this(5, TimeUnit.SECONDS);
    }

    ExpiringCache(long cleanupInterval, TimeUnit unit) {
        cleaner.scheduleAtFixedRate(this::cleanup, cleanupInterval, cleanupInterval, unit);
    }

    /**
     * Stores a value with the given time-to-live.
     *
     * @param key the cache key
     * @param value the value to store
     * @param ttlMillis the entry time-to-live in milliseconds
     */
    public void put(K key, V value, long ttlMillis) {
        long expireAt = System.currentTimeMillis() + ttlMillis;

        lock.lock();
        try {
            cache.put(key, new CacheEntry<>(value, expireAt));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the cached value for the given key, or {@code null} if the key is absent or expired.
     *
     * @param key the cache key
     * @return the cached value, or {@code null} if not available
     */
    public V get(K key) {
        lock.lock();
        try {
            CacheEntry<V> entry = cache.get(key);
            if (entry == null)
                return null;

            if (System.currentTimeMillis() > entry.expireAt) {
                cache.remove(key);
                return null;
            }

            return entry.value;
        } finally {
            lock.unlock();
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();

        lock.lock();
        try {
            Iterator<Map.Entry<K, CacheEntry<V>>> it = cache.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<K, CacheEntry<V>> entry = it.next();
                if (entry.getValue().expireAt <= now) {
                    it.remove();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    void shutdown() {
        cleaner.shutdownNow();
    }
}
