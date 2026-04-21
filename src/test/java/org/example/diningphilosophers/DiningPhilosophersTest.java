package org.example.diningphilosophers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.junit.jupiter.api.Test;

class DiningPhilosophersTest {

    private static final Runnable NO_OP = () -> {};

    @Test
    void executesCallbacksInExpectedOrder() throws InterruptedException {
        DiningPhilosophers diningPhilosophers = new DiningPhilosophers();
        List<String> steps = new ArrayList<>();

        diningPhilosophers.wantsToEat(
                0,
                () -> steps.add("pick-left"),
                () -> steps.add("pick-right"),
                () -> steps.add("eat"),
                () -> steps.add("put-left"),
                () -> steps.add("put-right")
        );

        assertEquals(List.of("pick-left", "pick-right", "eat", "put-right", "put-left"), steps);
    }

    @Test
    void neighboringPhilosophersCannotEatAtTheSameTime() throws InterruptedException {
        DiningPhilosophers diningPhilosophers = new DiningPhilosophers();
        CountDownLatch firstEating = new CountDownLatch(1);
        CountDownLatch allowFirstToFinish = new CountDownLatch(1);
        CountDownLatch secondFinished = new CountDownLatch(1);

        Thread first = new Thread(() -> runUnchecked(() -> diningPhilosophers.wantsToEat(
                0,
                NO_OP,
                NO_OP,
                () -> {
                    firstEating.countDown();
                    awaitUnchecked(allowFirstToFinish);
                },
                NO_OP,
                NO_OP
        )));

        Thread second = new Thread(() -> runUnchecked(() -> diningPhilosophers.wantsToEat(
                1,
                NO_OP,
                NO_OP,
                secondFinished::countDown,
                NO_OP,
                NO_OP
        )));

        first.start();
        assertTrue(firstEating.await(1, TimeUnit.SECONDS));

        second.start();
        assertFalse(secondFinished.await(150, TimeUnit.MILLISECONDS));

        allowFirstToFinish.countDown();

        second.join(1000);
        first.join(1000);
        assertFalse(first.isAlive());
        assertFalse(second.isAlive());
        assertEquals(0, secondFinished.getCount());
    }

    @Test
    void nonNeighboringPhilosophersCanEatConcurrently() throws InterruptedException {
        DiningPhilosophers diningPhilosophers = new DiningPhilosophers();
        CountDownLatch firstEating = new CountDownLatch(1);
        CountDownLatch allowFirstToFinish = new CountDownLatch(1);
        CountDownLatch secondFinished = new CountDownLatch(1);

        Thread first = new Thread(() -> runUnchecked(() -> diningPhilosophers.wantsToEat(
                0,
                NO_OP,
                NO_OP,
                () -> {
                    firstEating.countDown();
                    awaitUnchecked(allowFirstToFinish);
                },
                NO_OP,
                NO_OP
        )));

        Thread second = new Thread(() -> runUnchecked(() -> diningPhilosophers.wantsToEat(
                2,
                NO_OP,
                NO_OP,
                secondFinished::countDown,
                NO_OP,
                NO_OP
        )));

        first.start();
        assertTrue(firstEating.await(1, TimeUnit.SECONDS));

        second.start();
        assertTrue(secondFinished.await(1, TimeUnit.SECONDS));

        allowFirstToFinish.countDown();

        second.join(1000);
        first.join(1000);
        assertFalse(first.isAlive());
        assertFalse(second.isAlive());
    }

    @Test
    void allPhilosophersMakeProgressWithoutDeadlock() throws InterruptedException {
        DiningPhilosophers diningPhilosophers = new DiningPhilosophers();
        int mealsPerPhilosopher = 10;
        AtomicIntegerArray mealsEaten = new AtomicIntegerArray(5);
        CountDownLatch ready = new CountDownLatch(5);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(5);
        List<Thread> threads = new ArrayList<>();

        for (int philosopher = 0; philosopher < 5; philosopher++) {
            final int philosopherId = philosopher;
            Thread thread = new Thread(() -> {
                ready.countDown();
                awaitUnchecked(start);
                for (int meal = 0; meal < mealsPerPhilosopher; meal++) {
                    runUnchecked(() -> diningPhilosophers.wantsToEat(
                            philosopherId,
                            NO_OP,
                            NO_OP,
                            () -> mealsEaten.incrementAndGet(philosopherId),
                            NO_OP,
                            NO_OP
                    ));
                }
                done.countDown();
            });
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }

        assertTrue(ready.await(1, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(done.await(2, TimeUnit.SECONDS));

        for (Thread thread : threads) {
            thread.join(1000);
            assertFalse(thread.isAlive());
        }
        for (int philosopher = 0; philosopher < 5; philosopher++) {
            assertEquals(mealsPerPhilosopher, mealsEaten.get(philosopher));
        }
    }

    @Test
    void rejectsInvalidPhilosopherId() {
        DiningPhilosophers diningPhilosophers = new DiningPhilosophers();

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> diningPhilosophers.wantsToEat(5, NO_OP, NO_OP, NO_OP, NO_OP, NO_OP)
        );

        assertEquals("philosopher must be between 0 and 4", thrown.getMessage());
    }

    @FunctionalInterface
    private interface InterruptibleAction {
        void run() throws InterruptedException;
    }

    private void runUnchecked(InterruptibleAction action) {
        try {
            action.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void awaitUnchecked(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
