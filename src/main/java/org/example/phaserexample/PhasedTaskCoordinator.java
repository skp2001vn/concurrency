package org.example.phaserexample;

import java.util.concurrent.Phaser;

/**
 * Business logic: coordinates multi-step work where participants may join after
 * the coordinator starts and leave once their portion is complete.
 *
 * <p>Technique: wraps a {@link Phaser} because it supports dynamic registration
 * and deregistration across repeated phases. This fits phased workflows better
 * than a fixed barrier and lets the coordinator terminate when the phase limit
 * is reached or no parties remain.
 */
public class PhasedTaskCoordinator {

    private final int totalPhases;
    private final Phaser phaser;

    /**
     * Creates a coordinator that runs for the given number of phases.
     *
     * @param totalPhases the number of phases before the coordinator terminates
     */
    public PhasedTaskCoordinator(int totalPhases) {
        if (totalPhases <= 0) {
            throw new IllegalArgumentException("totalPhases must be positive");
        }

        this.totalPhases = totalPhases;
        this.phaser = new Phaser() {
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                return phase + 1 >= PhasedTaskCoordinator.this.totalPhases || registeredParties == 0;
            }
        };
    }

    /**
     * Registers a new participant for the current and future phases.
     *
     * @return a handle that the participant uses to arrive, wait, and deregister
     */
    public Participant registerParticipant() {
        int phase = phaser.register();
        if (phase < 0) {
            throw new IllegalStateException("coordinator is terminated");
        }
        return new Participant();
    }

    /**
     * Waits until the requested phase starts or the coordinator terminates.
     *
     * @param phase the phase number to wait for, starting at zero
     * @return the reached phase number, or a negative value if the coordinator terminated first
     */
    public int awaitPhaseStart(int phase) {
        if (phase < 0 || phase >= totalPhases) {
            throw new IllegalArgumentException("phase must be between 0 and totalPhases - 1");
        }

        int currentPhase = phaser.getPhase();
        while (currentPhase >= 0 && currentPhase < phase) {
            currentPhase = phaser.awaitAdvance(currentPhase);
        }
        return currentPhase;
    }

    /**
     * Returns the current phase number.
     *
     * @return the current phase, or a negative value if the coordinator has terminated
     */
    public int getCurrentPhase() {
        return phaser.getPhase();
    }

    /**
     * Returns the number of registered participants.
     *
     * @return the current number of registered parties
     */
    public int getRegisteredParties() {
        return phaser.getRegisteredParties();
    }

    /**
     * Returns whether the coordinator has terminated.
     *
     * @return {@code true} when the configured phases are complete or no parties remain
     */
    public boolean isTerminated() {
        return phaser.isTerminated();
    }

    /**
     * Participant handle used to synchronize with the other registered parties.
     */
    public final class Participant {
        private boolean active = true;

        private Participant() {
        }

        /**
         * Signals arrival for the current phase and waits until the phase advances.
         *
         * @return the phase this participant arrived in, or a negative value if the coordinator terminated
         */
        public synchronized int arriveAndAwaitAdvance() {
            ensureActive();

            int phase = phaser.arriveAndAwaitAdvance();
            if (phase < 0) {
                active = false;
            }
            return phase;
        }

        /**
         * Signals arrival for the current phase and removes this participant from future phases.
         *
         * @return the phase this participant arrived in, or a negative value if the coordinator terminated
         */
        public synchronized int arriveAndDeregister() {
            ensureActive();
            active = false;
            return phaser.arriveAndDeregister();
        }

        private void ensureActive() {
            if (!active) {
                throw new IllegalStateException("participant is not active");
            }
        }
    }
}
