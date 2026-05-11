package org.example.asyncjobqueue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Business logic: represents one background job and the number of failed
 * attempts already made.
 *
 * <p>Technique: keeps the executable task and retry counter together because
 * retry decisions need both pieces of state. The queue owns synchronization, so
 * this class stays a small data holder instead of spreading coordination logic.
 */
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
