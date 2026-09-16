package org.example.configurationregistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Business logic: lets application threads read service settings while administrators
 * apply batches of configuration changes without exposing partially applied updates.
 *
 * <p>Technique: protects a shared map with a {@link ReentrantReadWriteLock} so readers
 * can run concurrently while each writer applies its complete batch exclusively.
 * Immutable snapshots preserve a consistent view across services after unlocking;
 * separate calls to {@link #get(String)} may observe different updates.
 */
public class ConfigurationRegistry {

    private final Map<String, ServiceConfig> configurations = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates an empty configuration registry.
     */
    public ConfigurationRegistry() {
    }

    /**
     * Returns immutable settings for one service under the read lock.
     *
     * @param serviceName non-null, non-blank service name
     * @return the settings, or an empty optional if the service is unknown
     */
    public Optional<ServiceConfig> get(String serviceName) {
        validateServiceName(serviceName);
        lock.readLock().lock();
        try {
            return Optional.ofNullable(configurations.get(serviceName));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Copies all settings under one read lock, observing no partially applied batch.
     *
     * @return an immutable snapshot unaffected by subsequent updates
     */
    public Map<String, ServiceConfig> snapshot() {
        lock.readLock().lock();
        try {
            return Map.copyOf(configurations);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Adds or replaces service settings as one atomic batch, preserving unlisted services.
     * The batch is copied and validated before changing the registry. An empty batch
     * does nothing. Callers must not mutate the input while it is being copied.
     *
     * @param updates non-null map of non-blank service names to immutable settings
     * @throws NullPointerException if the map, a name, or a configuration is null
     * @throws IllegalArgumentException if a service name is blank
     */
    public void updateAll(Map<String, ServiceConfig> updates) {
        Map<String, ServiceConfig> batch = Map.copyOf(updates);
        batch.keySet().forEach(ConfigurationRegistry::validateServiceName);

        lock.writeLock().lock();
        try {
            configurations.putAll(batch);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static void validateServiceName(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName");
        if (serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName must not be blank");
        }
    }
}
