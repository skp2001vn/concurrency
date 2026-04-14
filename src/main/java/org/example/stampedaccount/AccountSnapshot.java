package org.example.stampedaccount;

/**
 * Immutable view of an account balance and update version.
 *
 * @param balanceInCents account balance at the time of the snapshot
 * @param version number of successful balance updates
 */
public record AccountSnapshot(long balanceInCents, long version) {
}
