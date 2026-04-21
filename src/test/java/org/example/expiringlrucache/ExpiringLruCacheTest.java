package org.example.expiringlrucache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ExpiringLruCacheTest {

    /**
     * Verifies that a cached value is returned while its TTL is still valid.
     */
    @Test
    void returnsValueBeforeExpiration() {
        ExpiringLruCache<Integer, String> cache = new ExpiringLruCache<>(2);

        cache.put(1, "A", 500);

        assertEquals("A", cache.get(1));
    }

    /**
     * Verifies that expired entries are no longer returned from the cache.
     */
    @Test
    void returnsNullAfterExpiration() throws InterruptedException {
        ExpiringLruCache<Integer, String> cache = new ExpiringLruCache<>(2);

        cache.put(1, "A", 100);
        Thread.sleep(150);

        assertNull(cache.get(1));
    }

    /**
     * Verifies that inserting beyond capacity evicts the least recently used entry.
     */
    @Test
    void evictsLeastRecentlyUsedEntryWhenCapacityExceeded() {
        ExpiringLruCache<Integer, String> cache = new ExpiringLruCache<>(2);

        cache.put(1, "A", 1_000);
        cache.put(2, "B", 1_000);
        cache.put(3, "C", 1_000);

        assertNull(cache.get(1));
        assertEquals("B", cache.get(2));
        assertEquals("C", cache.get(3));
    }

    /**
     * Verifies that reads refresh recency and protect the accessed entry from the next eviction.
     */
    @Test
    void getRefreshesRecencyBeforeEviction() {
        ExpiringLruCache<Integer, String> cache = new ExpiringLruCache<>(2);

        cache.put(1, "A", 1_000);
        cache.put(2, "B", 1_000);
        assertEquals("A", cache.get(1));

        cache.put(3, "C", 1_000);

        assertEquals("A", cache.get(1));
        assertNull(cache.get(2));
        assertEquals("C", cache.get(3));
    }

    /**
     * Verifies that overwriting an existing key updates both the stored value and TTL.
     */
    @Test
    void putOnExistingKeyUpdatesValueAndTtl() throws InterruptedException {
        ExpiringLruCache<Integer, String> cache = new ExpiringLruCache<>(2);

        cache.put(1, "A", 100);
        Thread.sleep(50);
        cache.put(1, "B", 300);
        Thread.sleep(100);

        assertEquals("B", cache.get(1));
    }
}
