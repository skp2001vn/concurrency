package org.example.stampedaccount;

/**
 * Business logic: immutable account view returned to callers that need both
 * balance and update version from the same point in time.
 *
 * <p>Technique: created from a {@link StampedAccount} optimistic or read-locked
 * snapshot because balance and version must come from the same observation. The
 * immutable record then lets callers share the result without extra
 * synchronization.
 *
 * @param balanceInCents account balance at the time of the snapshot
 * @param version number of successful balance updates
 */
public record AccountSnapshot(long balanceInCents, long version) {
}
