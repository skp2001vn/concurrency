package org.example.ratelimiter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LockBasedRateLimiterTest {

    /**
     * Verifies that requests beyond the limit are rejected within the active time window.
     */
    @Test
    void blocksRequestsAfterLimitIsReachedWithinWindow() {
        LockBasedRateLimiter limiter = new LockBasedRateLimiter(2, 200);

        assertTrue(limiter.allowRequest("user1"));
        assertTrue(limiter.allowRequest("user1"));
        assertFalse(limiter.allowRequest("user1"));
    }

    /**
     * Verifies that requests are accepted again after the rate-limit window expires.
     */
    @Test
    void allowsRequestsAgainAfterWindowExpires() throws InterruptedException {
        LockBasedRateLimiter limiter = new LockBasedRateLimiter(1, 100);

        assertTrue(limiter.allowRequest("user1"));
        assertFalse(limiter.allowRequest("user1"));

        Thread.sleep(150);

        assertTrue(limiter.allowRequest("user1"));
    }

    /**
     * Verifies that each user is rate-limited independently.
     */
    @Test
    void tracksLimitsSeparatelyPerUser() {
        LockBasedRateLimiter limiter = new LockBasedRateLimiter(1, 200);

        assertTrue(limiter.allowRequest("user1"));
        assertTrue(limiter.allowRequest("user2"));
        assertFalse(limiter.allowRequest("user1"));
        assertFalse(limiter.allowRequest("user2"));
    }
}
