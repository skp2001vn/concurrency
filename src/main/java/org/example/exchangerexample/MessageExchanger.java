package org.example.exchangerexample;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Business logic: lets two cooperating parties meet at a rendezvous point and
 * swap one payload, such as paired request/response handoff data.
 *
 * <p>Technique: delegates to {@link Exchanger} because the problem is a
 * two-party rendezvous with simultaneous value transfer. It removes manual
 * pairing logic, blocks until both parties arrive, and supports timed waits to
 * avoid hanging forever.
 *
 * @param <T> the exchanged value type
 */
public class MessageExchanger<T> {

    private final Exchanger<T> exchanger = new Exchanger<>();

    /**
     * Creates a reusable two-party exchanger.
     */
    public MessageExchanger() {
    }

    /**
     * Exchanges a value with another thread, blocking until the counterpart arrives.
     *
     * @param value the value to hand to the other thread
     * @return the value supplied by the other thread
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public T exchange(T value) throws InterruptedException {
        return exchanger.exchange(value);
    }

    /**
     * Exchanges a value with another thread, waiting only up to the given timeout.
     *
     * @param value the value to hand to the other thread
     * @param timeout the maximum time to wait for the counterpart
     * @return the value supplied by the other thread
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws TimeoutException if no counterpart arrives before the timeout expires
     */
    public T exchange(T value, Duration timeout) throws InterruptedException, TimeoutException {
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        return exchanger.exchange(value, timeout.toNanos(), TimeUnit.NANOSECONDS);
    }
}
