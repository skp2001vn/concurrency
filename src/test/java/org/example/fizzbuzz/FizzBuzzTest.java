package org.example.fizzbuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

class FizzBuzzTest {

    @Test
    void producesExpectedSequenceUpToFifteen() throws InterruptedException {
        List<String> output = runFizzBuzz(15);

        assertEquals(
                List.of(
                        "1", "2", "fizz", "4", "buzz",
                        "fizz", "7", "8", "fizz", "buzz",
                        "11", "fizz", "13", "14", "fizzbuzz"),
                output);
    }

    @Test
    void handlesSmallInputWithoutFizzOrBuzzValues() throws InterruptedException {
        assertEquals(List.of("1", "2"), runFizzBuzz(2));
    }

    @Test
    void continuesAfterAFizzBuzzValue() throws InterruptedException {
        assertEquals(
                List.of(
                        "1", "2", "fizz", "4", "buzz",
                        "fizz", "7", "8", "fizz", "buzz",
                        "11", "fizz", "13", "14", "fizzbuzz", "16"),
                runFizzBuzz(16));
    }

    private List<String> runFizzBuzz(int n) throws InterruptedException {
        FizzBuzz fizzBuzz = new FizzBuzz(n);
        List<String> output = new CopyOnWriteArrayList<>();

        Thread fizz = new Thread(() -> runUnchecked(() -> fizzBuzz.fizz(() -> output.add("fizz"))));
        Thread buzz = new Thread(() -> runUnchecked(() -> fizzBuzz.buzz(() -> output.add("buzz"))));
        Thread fizzbuzz =
                new Thread(() -> runUnchecked(() -> fizzBuzz.fizzbuzz(() -> output.add("fizzbuzz"))));
        Thread number =
                new Thread(() -> runUnchecked(() -> fizzBuzz.number(value -> output.add(String.valueOf(value)))));

        fizz.start();
        buzz.start();
        fizzbuzz.start();
        number.start();

        fizz.join(1000);
        buzz.join(1000);
        fizzbuzz.join(1000);
        number.join(1000);

        assertFalse(fizz.isAlive());
        assertFalse(buzz.isAlive());
        assertFalse(fizzbuzz.isAlive());
        assertFalse(number.isAlive());

        return new ArrayList<>(output);
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
}
