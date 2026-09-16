package org.example.configurationregistry;

/**
 * Business logic: stores a service's connection limits, request timeout, and retry limit.
 *
 * <p>Technique: uses an immutable record with constructor validation so callers can
 * safely retain settings after the registry releases its read lock.
 *
 * @param minConnections minimum connection count, including zero
 * @param maxConnections positive maximum connection count, at least the minimum
 * @param requestTimeoutMillis positive request timeout in milliseconds
 * @param maxRetries non-negative retry limit
 */
public record ServiceConfig(
        int minConnections,
        int maxConnections,
        long requestTimeoutMillis,
        int maxRetries) {

    /**
     * Creates validated, immutable service settings.
     *
     * @throws IllegalArgumentException if connection limits, timeout, or retries are invalid
     */
    public ServiceConfig {
        if (minConnections < 0 || maxConnections <= 0 || minConnections > maxConnections) {
            throw new IllegalArgumentException("invalid connection limits");
        }
        if (requestTimeoutMillis <= 0) {
            throw new IllegalArgumentException("requestTimeoutMillis must be positive");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
    }
}
