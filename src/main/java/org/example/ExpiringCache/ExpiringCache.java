package org.example.ExpiringCache;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class ExpiringCache<K, V> {

    private static class CacheEntry<V> {
        final V value;
        final long expireAt;

        CacheEntry(V value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }

    private final Map<K, CacheEntry<V>> cache = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    private final ScheduledExecutorService cleaner =
            Executors.newSingleThreadScheduledExecutor();

    public ExpiringCache() {
        cleaner.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.SECONDS);
    }

    public void put(K key, V value, long ttlMillis) {
        long expireAt = System.currentTimeMillis() + ttlMillis;

        lock.lock();
        try {
            cache.put(key, new CacheEntry<>(value, expireAt));
        } finally {
            lock.unlock();
        }
    }

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
}