package org.example.RateLimiter;

public class Main {

    public static void main(String[] args) {

        RateLimiter limiter = new SynchronizedRateLimiter(3, 1000);

        System.out.println(limiter.allowRequest("user1"));
        System.out.println(limiter.allowRequest("user1"));
        System.out.println(limiter.allowRequest("user1"));
        System.out.println(limiter.allowRequest("user1"));
    }
}
