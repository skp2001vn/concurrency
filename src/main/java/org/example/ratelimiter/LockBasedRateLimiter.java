package org.example.ratelimiter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Business logic: enforces a per-user request limit within a rolling time
 * window, as an API gateway or service endpoint might do.
 *
 * <p>Technique: keeps a per-user timestamp deque and {@link ReentrantLock} in a
 * {@link ConcurrentHashMap} because each user's sliding window is independent.
 * The explicit lock makes the compound trim/check/add operation atomic and
 * leaves room for lock-specific behavior such as try-locking or fairness.
 */
public class LockBasedRateLimiter implements RateLimiter {

    private final int limit;
    private final long windowSizeInMillis;

    private static class RateLimitState {
        final Deque<Long> timestamps = new ArrayDeque<>();
        final ReentrantLock lock = new ReentrantLock();
    }

    private final ConcurrentHashMap<String, RateLimitState> requestLog = new ConcurrentHashMap<>();

    /**
     * Creates a sliding-window limiter.
     *
     * @param limit the maximum number of requests allowed per user in the window
     * @param windowSizeInMillis the window size in milliseconds
     */
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
