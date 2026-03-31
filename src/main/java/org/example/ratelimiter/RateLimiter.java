package org.example.ratelimiter;

public interface RateLimiter {

    boolean allowRequest(String userId);
}
