package org.example.stampedaccount;

import java.util.concurrent.locks.StampedLock;

/**
 * A thread-safe bank account that uses {@link StampedLock}.
 *
 * <p>Deposits and withdrawals use the exclusive write lock. Balance reads and
 * snapshots first try an optimistic read and fall back to a regular read lock
 * if a concurrent write invalidates the optimistic snapshot.</p>
 */
public class StampedAccount {

    private final StampedLock lock = new StampedLock();

    private long balanceInCents;
    private long version;

    /**
     * Creates an account with the provided opening balance.
     *
     * @param openingBalanceInCents initial balance, in cents
     */
    public StampedAccount(long openingBalanceInCents) {
        if (openingBalanceInCents < 0) {
            throw new IllegalArgumentException("openingBalanceInCents must be non-negative");
        }
        this.balanceInCents = openingBalanceInCents;
    }

    /**
     * Deposits money into the account under the write lock.
     *
     * @param amountInCents positive amount to deposit, in cents
     */
    public void deposit(long amountInCents) {
        validatePositive(amountInCents);

        long stamp = lock.writeLock();
        try {
            balanceInCents = Math.addExact(balanceInCents, amountInCents);
            version++;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Withdraws money if enough balance is available.
     *
     * @param amountInCents positive amount to withdraw, in cents
     * @return true if the withdrawal succeeded
     */
    public boolean withdraw(long amountInCents) {
        validatePositive(amountInCents);

        long stamp = lock.writeLock();
        try {
            if (balanceInCents < amountInCents) {
                return false;
            }

            balanceInCents -= amountInCents;
            version++;
            return true;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Returns the current balance using an optimistic read when possible.
     *
     * @return current balance, in cents
     */
    public long getBalanceInCents() {
        long stamp = lock.tryOptimisticRead();
        long currentBalance = balanceInCents;

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                currentBalance = balanceInCents;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        return currentBalance;
    }

    /**
     * Returns a consistent balance/version pair using optimistic reads.
     *
     * @return immutable account snapshot
     */
    public AccountSnapshot snapshot() {
        long stamp = lock.tryOptimisticRead();
        long currentBalance = balanceInCents;
        long currentVersion = version;

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                currentBalance = balanceInCents;
                currentVersion = version;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        return new AccountSnapshot(currentBalance, currentVersion);
    }

    private static void validatePositive(long amountInCents) {
        if (amountInCents <= 0) {
            throw new IllegalArgumentException("amountInCents must be positive");
        }
    }
}
