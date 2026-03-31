package org.example.RateLimiter;

public interface RateLimiter {

    boolean allowRequest(String userId);
}
