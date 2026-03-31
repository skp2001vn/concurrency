package org.example.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A sliding-window rate limiter that uses per-user synchronized queues to
 * serialize request tracking and enforce a maximum number of requests within
 * a fixed time window.
 */
public class SynchronizedRateLimiter implements RateLimiter {

    private final int limit;
    private final long windowSizeInMillis;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> requestLog;

    public SynchronizedRateLimiter(int limit, long windowSizeInMillis) {
        this.limit = limit;
        this.windowSizeInMillis = windowSizeInMillis;
        this.requestLog = new ConcurrentHashMap<>();
    }

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
