package org.example.ratelimiter;

/**
 * Business logic: decides whether a user's request should be admitted under a
 * configured request quota.
 *
 * <p>Technique: implementations maintain per-user sliding windows because recent
 * timestamps are enough to decide admission. Coordinating updates with
 * synchronized queues or explicit locks keeps each user's decision atomic while
 * allowing different users to be checked independently.
 */
public interface RateLimiter {

    /**
     * Determines whether a request for the given user should be allowed under the active limit.
     *
     * @param userId the caller identifier
     * @return {@code true} if the request is allowed, otherwise {@code false}
     */
    boolean allowRequest(String userId);
}
