package org.example.ratelimiter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A sliding-window rate limiter that uses a dedicated reentrant lock per user
 * to coordinate request tracking and enforce request limits within a fixed
 * time window.
 */
public class LockBasedRateLimiter implements RateLimiter {

    private final int limit;
    private final long windowSizeInMillis;

    private static class RateLimitState {
        final Deque<Long> timestamps = new ArrayDeque<>();
        final ReentrantLock lock = new ReentrantLock();
    }

    private final ConcurrentHashMap<String, RateLimitState> requestLog = new ConcurrentHashMap<>();

    public LockBasedRateLimiter(int limit, long windowSizeInMillis) {
        this.limit = limit;
        this.windowSizeInMillis = windowSizeInMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();

        RateLimitState state = requestLog.computeIfAbsent(userId, id -> new RateLimitState());
        state.lock.lock();
        try {
            while (!state.timestamps.isEmpty()
                    && now - state.timestamps.peekFirst() > windowSizeInMillis) {
                state.timestamps.pollFirst();
            }

            if (state.timestamps.size() < limit) {
                state.timestamps.addLast(now);
                return true;
            }

            return false;
        } finally {
            state.lock.unlock();
        }
    }
}
