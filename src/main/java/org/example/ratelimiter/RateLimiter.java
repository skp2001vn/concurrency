package org.example.ratelimiter;

public interface RateLimiter {

    /**
     * Determines whether a request for the given user should be allowed under the active limit.
     *
     * @param userId the caller identifier
     * @return {@code true} if the request is allowed, otherwise {@code false}
     */
    boolean allowRequest(String userId);
}
