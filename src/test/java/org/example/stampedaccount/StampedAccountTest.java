package org.example.stampedaccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class StampedAccountTest {

    @Test
    void rejectsNegativeOpeningBalance() {
        assertThrows(IllegalArgumentException.class, () -> new StampedAccount(-1));
    }

    @Test
    void depositIncreasesBalanceAndVersion() {
        StampedAccount account = new StampedAccount(100);

        account.deposit(50);

        AccountSnapshot snapshot = account.snapshot();
        assertEquals(150, snapshot.balanceInCents());
        assertEquals(1, snapshot.version());
    }

    @Test
    void withdrawFailsWhenFundsAreInsufficient() {
        StampedAccount account = new StampedAccount(100);

        assertFalse(account.withdraw(150));

        AccountSnapshot snapshot = account.snapshot();
        assertEquals(100, snapshot.balanceInCents());
        assertEquals(0, snapshot.version());
    }

    @Test
    void withdrawDecreasesBalanceAndIncrementsVersion() {
        StampedAccount account = new StampedAccount(100);

        assertTrue(account.withdraw(40));

        AccountSnapshot snapshot = account.snapshot();
        assertEquals(60, snapshot.balanceInCents());
        assertEquals(1, snapshot.version());
    }

    @Test
    void rejectsNonPositiveAmounts() {
        StampedAccount account = new StampedAccount(100);

        assertThrows(IllegalArgumentException.class, () -> account.deposit(0));
        assertThrows(IllegalArgumentException.class, () -> account.deposit(-1));
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(0));
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(-1));
    }

    @Test
    void concurrentDepositsProduceExpectedBalance() throws Exception {
        StampedAccount account = new StampedAccount(0);
        int deposits = 100;

        runConcurrently(deposits, () -> {
            account.deposit(25);
            return null;
        });

        AccountSnapshot snapshot = account.snapshot();
        assertEquals(2_500, snapshot.balanceInCents());
        assertEquals(deposits, snapshot.version());
    }

    @Test
    void concurrentWithdrawalsDoNotOverdraw() throws Exception {
        StampedAccount account = new StampedAccount(1_000);
        int attempts = 30;

        List<Boolean> results = runConcurrently(attempts, () -> account.withdraw(100));

        long successfulWithdrawals = results.stream()
                .filter(Boolean::booleanValue)
                .count();

        AccountSnapshot snapshot = account.snapshot();
        assertEquals(10, successfulWithdrawals);
        assertEquals(0, snapshot.balanceInCents());
        assertEquals(10, snapshot.version());
    }

    @Test
    void snapshotsStayConsistentDuringConcurrentWrites() throws Exception {
        StampedAccount account = new StampedAccount(0);
        int writes = 200;
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?> writer = executor.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < writes; i++) {
                        account.deposit(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            Future<List<AccountSnapshot>> reader = executor.submit(() -> {
                List<AccountSnapshot> snapshots = new ArrayList<>();
                start.await();
                while (!writer.isDone()) {
                    snapshots.add(account.snapshot());
                }
                snapshots.add(account.snapshot());
                return snapshots;
            });

            start.countDown();
            writer.get(1, TimeUnit.SECONDS);
            List<AccountSnapshot> snapshots = reader.get(1, TimeUnit.SECONDS);

            for (AccountSnapshot snapshot : snapshots) {
                assertEquals(snapshot.balanceInCents(), snapshot.version());
            }
            assertEquals(writes, account.getBalanceInCents());
        } finally {
            executor.shutdownNow();
        }
    }

    private static <T> List<T> runConcurrently(int tasks, Callable<T> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks);
        try {
            List<Callable<T>> callables = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                callables.add(task);
            }

            List<T> results = new ArrayList<>();
            List<Future<T>> futures = executor.invokeAll(callables);
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }
}
