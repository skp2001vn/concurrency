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

    private Semaphore numberSem = new Semaphore(1);
    private Semaphore fizzSem = new Semaphore(0);
    private Semaphore buzzSem = new Semaphore(0);
    private Semaphore fizzbuzzSem = new Semaphore(0);

    public FizzBuzz(int n) {
        this.n = n;
    }

    public void fizz(Runnable printFizz) throws InterruptedException {

        for (int i = 3; i <= n; i += 3) {
            if (i % 15 == 0) continue;

            fizzSem.acquire();
            printFizz.run();
            numberSem.release();
        }
    }

    public void buzz(Runnable printBuzz) throws InterruptedException {

        for (int i = 5; i <= n; i += 5) {
            if (i % 15 == 0) continue;

            buzzSem.acquire();
            printBuzz.run();
            numberSem.release();
        }
    }

    public void fizzbuzz(Runnable printFizzBuzz) throws InterruptedException {

        for (int i = 15; i <= n; i += 15) {
            fizzbuzzSem.acquire();
            printFizzBuzz.run();
            numberSem.release();
        }
    }

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
