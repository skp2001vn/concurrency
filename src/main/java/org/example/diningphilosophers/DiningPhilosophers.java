package org.example.diningphilosophers;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Coordinates five philosophers that share five forks arranged in a circle.
 *
 * <p>Each fork is protected by its own {@link ReentrantLock}. Deadlock is
 * prevented by allowing at most four philosophers to compete for forks at the
 * same time, which breaks the circular wait condition while still allowing
 * non-neighboring philosophers to eat concurrently.
 */
public class DiningPhilosophers {

    private static final int PHILOSOPHER_COUNT = 5;

    private final Semaphore room = new Semaphore(PHILOSOPHER_COUNT - 1, true);
    private final ReentrantLock[] forks = new ReentrantLock[PHILOSOPHER_COUNT];

    /**
     * Creates a new dining philosophers coordinator.
     */
    public DiningPhilosophers() {
        for (int i = 0; i < PHILOSOPHER_COUNT; i++) {
            forks[i] = new ReentrantLock(true);
        }
    }

    /**
     * Waits for the given philosopher to acquire both adjacent forks, runs the
     * callbacks for picking up forks and eating, then releases the forks.
     *
     * <p>Callbacks are invoked in this order: pick left fork, pick right fork,
     * eat, put right fork, put left fork.
     *
     * @param philosopher the philosopher id, from {@code 0} to {@code 4}
     * @param pickLeftFork callback invoked after the left fork is acquired
     * @param pickRightFork callback invoked after the right fork is acquired
     * @param eat callback invoked while both forks are held
     * @param putLeftFork callback invoked before the left fork is released
     * @param putRightFork callback invoked before the right fork is released
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void wantsToEat(
            int philosopher,
            Runnable pickLeftFork,
            Runnable pickRightFork,
            Runnable eat,
            Runnable putLeftFork,
            Runnable putRightFork
    ) throws InterruptedException {
        validatePhilosopher(philosopher);

        int leftFork = philosopher;
        int rightFork = (philosopher + 1) % PHILOSOPHER_COUNT;

        boolean roomAcquired = false;
        boolean leftForkHeld = false;
        boolean rightForkHeld = false;
        boolean completedNormally = false;
        Throwable failure = null;

        room.acquire();
        roomAcquired = true;
        try {
            forks[leftFork].lockInterruptibly();
            leftForkHeld = true;
            pickLeftFork.run();

            forks[rightFork].lockInterruptibly();
            rightForkHeld = true;
            pickRightFork.run();

            eat.run();
            completedNormally = true;
        } catch (InterruptedException e) {
            failure = e;
            throw e;
        } catch (RuntimeException | Error e) {
            failure = e;
            throw e;
        } finally {
            if (rightForkHeld) {
                try {
                    putRightFork.run();
                } catch (Throwable t) {
                    failure = mergeFailure(failure, t);
                } finally {
                    forks[rightFork].unlock();
                }
            }

            if (leftForkHeld) {
                try {
                    putLeftFork.run();
                } catch (Throwable t) {
                    failure = mergeFailure(failure, t);
                } finally {
                    forks[leftFork].unlock();
                }
            }

            if (roomAcquired) {
                room.release();
            }

            if (completedNormally) {
                rethrowUnchecked(failure);
            }
        }
    }

    private void validatePhilosopher(int philosopher) {
        if (philosopher < 0 || philosopher >= PHILOSOPHER_COUNT) {
            throw new IllegalArgumentException("philosopher must be between 0 and 4");
        }
    }

    private Throwable mergeFailure(Throwable existing, Throwable next) {
        if (existing == null) {
            return next;
        }

        existing.addSuppressed(next);
        return existing;
    }

    private void rethrowUnchecked(Throwable failure) {
        if (failure == null) {
            return;
        }

        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure instanceof Error error) {
            throw error;
        }

        throw new IllegalStateException("Unexpected callback failure", failure);
    }
}
