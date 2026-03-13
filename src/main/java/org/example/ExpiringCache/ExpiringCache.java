package org.example.ExpiringCache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExpiringCache<K, V> {

    private static class CacheEntry<V> {
        V value;
        long expireAt;

        CacheEntry(V value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }

    private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public ExpiringCache() {

        cleaner.scheduleAtFixedRate(
                this::cleanup,
                5,
                5,
                TimeUnit.SECONDS
        );
    }

    public void put(K key, V value, long ttlMillis) {

        long expireAt = System.currentTimeMillis() + ttlMillis;
        cache.put(key, new CacheEntry<>(value, expireAt));
    }

    public V get(K key) {

        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        if (System.currentTimeMillis() > entry.expireAt) {
            cache.remove(key);
            return null;
        }

        return entry.value;
    }

    private void cleanup() {

        long now = System.currentTimeMillis();
        for (var entry : cache.entrySet()) {
            if (entry.getValue().expireAt <= now) {
                cache.remove(entry.getKey());
            }
        }
    }
}