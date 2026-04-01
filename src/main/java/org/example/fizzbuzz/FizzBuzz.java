package org.example.fizzbuzz;

import java.util.concurrent.Semaphore;
import java.util.function.IntConsumer;

/**
 * Coordinates four threads to print the classic FizzBuzz sequence from {@code 1} to {@code n}.
 *
 * <p>One thread prints numbers, and three specialized threads print {@code fizz}, {@code buzz},
 * and {@code fizzbuzz}. Semaphores ensure that exactly one output is produced for each number and
 * that outputs appear in ascending order.
 */
public class FizzBuzz {

    private int n;

    private final Semaphore numberSem = new Semaphore(1);
    private final Semaphore fizzSem = new Semaphore(0);
    private final Semaphore buzzSem = new Semaphore(0);
    private final Semaphore fizzbuzzSem = new Semaphore(0);

    /**
     * Creates a FizzBuzz coordinator that prints values from {@code 1} to {@code n}.
     *
     * @param n the inclusive upper bound of the sequence
     */
    public FizzBuzz(int n) {
        this.n = n;
    }

    /**
     * Prints {@code fizz} for multiples of 3 that are not multiples of 5.
     *
     * @param printFizz callback used to emit {@code fizz}
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void fizz(Runnable printFizz) throws InterruptedException {

        for (int i = 3; i <= n; i += 3) {
            if (i % 15 == 0) continue;

            fizzSem.acquire();
            printFizz.run();
            numberSem.release();
        }
    }

    /**
     * Prints {@code buzz} for multiples of 5 that are not multiples of 3.
     *
     * @param printBuzz callback used to emit {@code buzz}
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void buzz(Runnable printBuzz) throws InterruptedException {

        for (int i = 5; i <= n; i += 5) {
            if (i % 15 == 0) continue;

            buzzSem.acquire();
            printBuzz.run();
            numberSem.release();
        }
    }

    /**
     * Prints {@code fizzbuzz} for values divisible by both 3 and 5.
     *
     * @param printFizzBuzz callback used to emit {@code fizzbuzz}
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void fizzbuzz(Runnable printFizzBuzz) throws InterruptedException {

        for (int i = 15; i <= n; i += 15) {
            fizzbuzzSem.acquire();
            printFizzBuzz.run();
            numberSem.release();
        }
    }

    /**
     * Prints plain numbers and delegates divisible values to the specialized worker threads.
     *
     * @param printNumber callback used to emit a number
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void number(IntConsumer printNumber) throws InterruptedException {

        for (int i = 1; i <= n; i++) {
            numberSem.acquire();

            if (i % 15 == 0) {
                fizzbuzzSem.release();
            } else if (i % 3 == 0) {
                fizzSem.release();
            } else if (i % 5 == 0) {
                buzzSem.release();
            } else {
                printNumber.accept(i);
                numberSem.release();
            }
        }
    }
}
