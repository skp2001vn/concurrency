package org.example.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RateLimiter {

    private final int limit;
    private final long windowSizeInMillis;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> requestLog;

    public RateLimiter(int limit, long windowSizeInMillis) {
        this.limit = limit;
        this.windowSizeInMillis = windowSizeInMillis;
        this.requestLog = new ConcurrentHashMap<>();
    }

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