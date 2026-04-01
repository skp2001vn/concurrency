package org.example.asyncjobqueue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Job {

    private final String id;
    private final Runnable task;
    private int retries;

    /**
     * Increments the retry counter after a failed attempt.
     */
    public void incrementRetry() {
        retries++;
    }
}
