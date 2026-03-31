package org.example.expiringcache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ExpiringCacheTest {

    private ExpiringCache<String, String> cache;

    @AfterEach
    void tearDown() {
        if (cache != null) {
            cache.shutdown();
        }
    }

    @Test
    void returnsValueBeforeExpiration() {
        cache = new ExpiringCache<>(50, TimeUnit.MILLISECONDS);

        cache.put("user", "Alice", 500);

        assertEquals("Alice", cache.get("user"));
    }

    @Test
    void returnsNullAfterEntryExpiresOnAccess() throws InterruptedException {
        cache = new ExpiringCache<>(50, TimeUnit.MILLISECONDS);

        cache.put("user", "Alice", 100);
        Thread.sleep(150);

        assertNull(cache.get("user"));
    }

    @Test
    void putOverwritesValueAndExpiration() throws InterruptedException {
        cache = new ExpiringCache<>(50, TimeUnit.MILLISECONDS);

        cache.put("user", "Alice", 100);
        Thread.sleep(50);
        cache.put("user", "Bob", 300);
        Thread.sleep(100);

        assertEquals("Bob", cache.get("user"));
    }

    @Test
    void backgroundCleanupRemovesExpiredEntry() throws InterruptedException {
        cache = new ExpiringCache<>(25, TimeUnit.MILLISECONDS);

        cache.put("user", "Alice", 40);
        Thread.sleep(150);

        assertNull(cache.get("user"));
    }
}
