package org.example.exchangerexample;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Coordinates two threads that rendezvous and swap one value each.
 *
 * <p>The exchanger blocks each caller until its counterpart arrives. Once both
 * parties reach the rendezvous point, each thread receives the other thread's
 * value. The same exchanger can be reused across multiple exchange rounds.
 *
 * @param <T> the exchanged value type
 */
public class MessageExchanger<T> {

    private final Exchanger<T> exchanger = new Exchanger<>();

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
