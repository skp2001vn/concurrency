package org.example.configurationregistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ConfigurationRegistryTest {

    private static final ServiceConfig ORIGINAL = new ServiceConfig(5, 20, 1_000, 2);
    private static final ServiceConfig UPDATED = new ServiceConfig(30, 50, 2_000, 3);

    /**
     * Verifies that a new registry has no settings and unknown services return empty.
     */
    @Test
    void startsEmpty() {
        ConfigurationRegistry registry = new ConfigurationRegistry();

        assertTrue(registry.get("payment").isEmpty());
        assertTrue(registry.snapshot().isEmpty());
    }

    /**
     * Verifies that batches add and replace services while preserving unlisted services.
     */
    @Test
    void updatesServicesAndPreservesUnlistedSettings() {
        ConfigurationRegistry registry = new ConfigurationRegistry();
        registry.updateAll(Map.of("payment", ORIGINAL, "shipping", ORIGINAL));

        registry.updateAll(Map.of("payment", UPDATED, "search", UPDATED));

        assertEquals(UPDATED, registry.get("payment").orElseThrow());
        assertEquals(Map.of("payment", UPDATED, "shipping", ORIGINAL, "search", UPDATED),
                registry.snapshot());
    }

    /**
     * Verifies that empty updates leave existing settings intact.
     */
    @Test
    void acceptsEmptyBatch() {
        ConfigurationRegistry registry = new ConfigurationRegistry();
        registry.updateAll(Map.of("payment", ORIGINAL));

        registry.updateAll(Map.of());

        assertEquals(Map.of("payment", ORIGINAL), registry.snapshot());
    }

    /**
     * Verifies that snapshots are immutable and retain their values after later updates.
     */
    @Test
    void snapshotsAreImmutableAndIndependent() {
        ConfigurationRegistry registry = new ConfigurationRegistry();
        registry.updateAll(Map.of("payment", ORIGINAL));
        Map<String, ServiceConfig> snapshot = registry.snapshot();

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.put("shipping", UPDATED));
        registry.updateAll(Map.of("payment", UPDATED));

        assertEquals(Map.of("payment", ORIGINAL), snapshot);
        assertEquals(UPDATED, registry.get("payment").orElseThrow());
    }

    /**
     * Verifies that changing the caller's map after an update cannot change registry state.
     */
    @Test
    void doesNotRetainMutableInputMap() {
        ConfigurationRegistry registry = new ConfigurationRegistry();
        Map<String, ServiceConfig> updates = new HashMap<>(Map.of("payment", ORIGINAL));
        registry.updateAll(updates);

        updates.put("payment", UPDATED);
        updates.put("shipping", UPDATED);

        assertEquals(Map.of("payment", ORIGINAL), registry.snapshot());
    }

    /**
     * Verifies that invalid names reject the entire batch before any setting changes.
     */
    @Test
    void rejectsBlankNamesWithoutPartialUpdates() {
        ConfigurationRegistry registry = new ConfigurationRegistry();
        registry.updateAll(Map.of("payment", ORIGINAL));
        Map<String, ServiceConfig> updates = new LinkedHashMap<>();
        updates.put("payment", UPDATED);
        updates.put("  ", UPDATED);

        assertThrows(IllegalArgumentException.class, () -> registry.updateAll(updates));
        assertThrows(IllegalArgumentException.class, () -> registry.get(""));
        assertEquals(Map.of("payment", ORIGINAL), registry.snapshot());
    }

    /**
     * Verifies that null inputs and entries are rejected without modifying the registry.
     */
    @Test
    void rejectsNullInputsWithoutPartialUpdates() {
        ConfigurationRegistry registry = new ConfigurationRegistry();
        registry.updateAll(Map.of("payment", ORIGINAL));
        Map<String, ServiceConfig> updates = new LinkedHashMap<>();
        updates.put("payment", UPDATED);
        updates.put("shipping", null);

        assertThrows(NullPointerException.class, () -> registry.get(null));
        assertThrows(NullPointerException.class, () -> registry.updateAll(null));
        assertThrows(NullPointerException.class, () -> registry.updateAll(updates));
        updates.remove("shipping");
        updates.put(null, UPDATED);
        assertThrows(NullPointerException.class, () -> registry.updateAll(updates));
        assertEquals(Map.of("payment", ORIGINAL), registry.snapshot());
    }

    /**
     * Verifies setting constraints, including valid zero minimum connections and retries.
     */
    @Test
    void validatesServiceSettings() {
        ServiceConfig minimum = new ServiceConfig(0, 1, 1, 0);
        assertEquals(0, minimum.minConnections());
        assertEquals(0, minimum.maxRetries());
        assertThrows(IllegalArgumentException.class, () -> new ServiceConfig(-1, 20, 1_000, 2));
        assertThrows(IllegalArgumentException.class, () -> new ServiceConfig(0, 0, 1_000, 2));
        assertThrows(IllegalArgumentException.class, () -> new ServiceConfig(30, 20, 1_000, 2));
        assertThrows(IllegalArgumentException.class, () -> new ServiceConfig(5, 20, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> new ServiceConfig(5, 20, -1, 2));
        assertThrows(IllegalArgumentException.class, () -> new ServiceConfig(5, 20, 1_000, -1));
    }

    /**
     * Verifies that concurrent readers see complete batches across related services.
     */
    @Test
    void snapshotsStayConsistentDuringBatchUpdates() throws Exception {
        ConfigurationRegistry registry = new ConfigurationRegistry();
        registry.updateAll(Map.of("payment", ORIGINAL, "shipping", ORIGINAL));
        int rounds = 500;
        CyclicBarrier roundStart = new CyclicBarrier(4);
        ExecutorService executor = Executors.newFixedThreadPool(4);

        try {
            List<Future<?>> tasks = new ArrayList<>();
            tasks.add(executor.submit(() -> {
                for (int i = 0; i < rounds; i++) {
                    roundStart.await(5, TimeUnit.SECONDS);
                    ServiceConfig config = i % 2 == 0 ? UPDATED : ORIGINAL;
                    registry.updateAll(Map.of("payment", config, "shipping", config));
                }
                return null;
            }));
            for (int reader = 0; reader < 3; reader++) {
                tasks.add(executor.submit(() -> {
                    for (int i = 0; i < rounds; i++) {
                        roundStart.await(5, TimeUnit.SECONDS);
                        Map<String, ServiceConfig> snapshot = registry.snapshot();
                        assertEquals(2, snapshot.size());
                        assertEquals(snapshot.get("payment"), snapshot.get("shipping"));
                        assertTrue(registry.get("payment").isPresent());
                    }
                    return null;
                }));
            }
            for (Future<?> task : tasks) {
                task.get(10, TimeUnit.SECONDS);
            }
            assertEquals(Map.of("payment", ORIGINAL, "shipping", ORIGINAL), registry.snapshot());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    /**
     * Verifies that concurrent writers preserve every independently added service.
     */
    @Test
    void concurrentBatchesDoNotLoseServices() throws Exception {
        ConfigurationRegistry registry = new ConfigurationRegistry();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<?>> tasks = new ArrayList<>();
            for (int worker = 0; worker < 4; worker++) {
                int workerId = worker;
                tasks.add(executor.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    for (int i = 0; i < 100; i++) {
                        registry.updateAll(Map.of(workerId + ":" + i, ORIGINAL));
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> task : tasks) {
                task.get(10, TimeUnit.SECONDS);
            }
            Map<String, ServiceConfig> snapshot = registry.snapshot();
            assertEquals(400, snapshot.size());
            for (int worker = 0; worker < 4; worker++) {
                for (int i = 0; i < 100; i++) {
                    assertEquals(ORIGINAL, snapshot.get(worker + ":" + i));
                }
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
