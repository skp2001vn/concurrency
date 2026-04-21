package org.example.exchangerexample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class MessageExchangerTest {

    /**
     * Verifies that two threads receive each other's values at the rendezvous point.
     */
    @Test
    void swapsValuesBetweenTwoThreads() throws Exception {
        MessageExchanger<String> exchanger = new MessageExchanger<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> first = executor.submit(() -> exchanger.exchange("ping"));
            Future<String> second = executor.submit(() -> exchanger.exchange("pong"));

            assertEquals("pong", first.get(1, TimeUnit.SECONDS));
            assertEquals("ping", second.get(1, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Verifies that one caller remains blocked until the matching thread arrives.
     */
    @Test
    void blocksUntilTheOtherThreadArrives() throws Exception {
        MessageExchanger<String> exchanger = new MessageExchanger<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> first = executor.submit(() -> exchanger.exchange("first"));

            assertThrows(TimeoutException.class, () -> first.get(150, TimeUnit.MILLISECONDS));

            Future<String> second = executor.submit(() -> exchanger.exchange("second"));

            assertEquals("first", second.get(1, TimeUnit.SECONDS));
            assertEquals("second", first.get(1, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Verifies that a timed exchange fails when no counterpart arrives in time.
     */
    @Test
    void timesOutWhenNoPartnerArrives() {
        MessageExchanger<String> exchanger = new MessageExchanger<>();

        assertThrows(
                TimeoutException.class,
                () -> exchanger.exchange("lonely", Duration.ofMillis(100))
        );
    }

    /**
     * Verifies that the same exchanger instance can be reused across multiple rounds.
     */
    @Test
    void canBeReusedAcrossMultipleRounds() throws Exception {
        MessageExchanger<String> exchanger = new MessageExchanger<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> roundOneLeft = executor.submit(() -> exchanger.exchange("round-1-left"));
            Future<String> roundOneRight = executor.submit(() -> exchanger.exchange("round-1-right"));

            assertEquals("round-1-right", roundOneLeft.get(1, TimeUnit.SECONDS));
            assertEquals("round-1-left", roundOneRight.get(1, TimeUnit.SECONDS));

            Future<String> roundTwoLeft = executor.submit(() -> exchanger.exchange("round-2-left"));
            Future<String> roundTwoRight = executor.submit(() -> exchanger.exchange("round-2-right"));

            assertEquals("round-2-right", roundTwoLeft.get(1, TimeUnit.SECONDS));
            assertEquals("round-2-left", roundTwoRight.get(1, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Verifies that invalid timeout arguments are rejected.
     */
    @Test
    void rejectsInvalidTimeoutArguments() {
        MessageExchanger<String> exchanger = new MessageExchanger<>();

        IllegalArgumentException zeroTimeout = assertThrows(
                IllegalArgumentException.class,
                () -> exchanger.exchange("value", Duration.ZERO)
        );
        assertEquals("timeout must be positive", zeroTimeout.getMessage());

        NullPointerException nullTimeout = assertThrows(
                NullPointerException.class,
                () -> exchanger.exchange("value", null)
        );
        assertEquals("timeout must not be null", nullTimeout.getMessage());
    }
}
