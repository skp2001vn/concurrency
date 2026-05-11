package org.example.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Business logic: enforces a per-user request limit within a rolling time
 * window, as an API gateway or service endpoint might do.
 *
 * <p>Technique: stores timestamp queues in a {@link ConcurrentHashMap} because
 * each user has independent request history. Synchronizing on the user's queue
 * keeps trimming and admission atomic with minimal machinery, while different
 * users can still proceed concurrently.
 */
public class SynchronizedRateLimiter implements RateLimiter {

    private final int limit;
    private final long windowSizeInMillis;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> requestLog;

    /**
     * Creates a sliding-window limiter.
     *
     * @param limit the maximum number of requests allowed per user in the window
     * @param windowSizeInMillis the window size in milliseconds
     */
    public SynchronizedRateLimiter(int limit, long windowSizeInMillis) {
        this.limit = limit;
        this.windowSizeInMillis = windowSizeInMillis;
        this.requestLog = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        requestLog.putIfAbsent(userId, new ConcurrentLinkedQueue<>());
        ConcurrentLinkedQueue<Long> timestamps = requestLog.get(userId);

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peek() > windowSizeInMillis) {
                timestamps.poll();
            }

            if (timestamps.size() < limit) {
                timestamps.add(now);
                return true;
            }

            return false;
        }
    }
}
