package org.example.phaserexample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PhasedTaskCoordinatorTest {

    /**
     * Verifies that a phase does not advance until every registered participant arrives.
     */
    @Test
    void waitsForAllRegisteredParticipantsBeforeAdvancing() throws InterruptedException {
        PhasedTaskCoordinator coordinator = new PhasedTaskCoordinator(3);
        PhasedTaskCoordinator.Participant first = coordinator.registerParticipant();
        PhasedTaskCoordinator.Participant second = coordinator.registerParticipant();

        CountDownLatch firstWaiting = new CountDownLatch(1);
        CountDownLatch firstAdvanced = new CountDownLatch(1);

        Thread firstThread = new Thread(() -> {
            firstWaiting.countDown();
            first.arriveAndAwaitAdvance();
            firstAdvanced.countDown();
            first.arriveAndDeregister();
        });
        firstThread.start();

        assertTrue(firstWaiting.await(1, TimeUnit.SECONDS));
        assertFalse(firstAdvanced.await(150, TimeUnit.MILLISECONDS));

        second.arriveAndAwaitAdvance();

        assertTrue(firstAdvanced.await(1, TimeUnit.SECONDS));
        assertEquals(1, coordinator.getCurrentPhase());

        second.arriveAndDeregister();
        firstThread.join(1000);
        assertFalse(firstThread.isAlive());
    }

    /**
     * Verifies that a participant can register after phase zero and still join the next phase.
     */
    @Test
    void allowsLateRegistrationForAFuturePhase() throws InterruptedException {
        PhasedTaskCoordinator coordinator = new PhasedTaskCoordinator(3);
        PhasedTaskCoordinator.Participant initial = coordinator.registerParticipant();

        CountDownLatch enteredPhaseOne = new CountDownLatch(1);
        CountDownLatch allowInitialArrival = new CountDownLatch(1);
        CountDownLatch initialFinishedPhaseOne = new CountDownLatch(1);
        CountDownLatch allowDeregistration = new CountDownLatch(1);

        Thread initialThread = new Thread(() -> {
            initial.arriveAndAwaitAdvance();
            enteredPhaseOne.countDown();
            await(allowInitialArrival);
            initial.arriveAndAwaitAdvance();
            initialFinishedPhaseOne.countDown();
            await(allowDeregistration);
            initial.arriveAndDeregister();
        });
        initialThread.start();

        assertEquals(1, coordinator.awaitPhaseStart(1));
        assertTrue(enteredPhaseOne.await(1, TimeUnit.SECONDS));

        PhasedTaskCoordinator.Participant late = coordinator.registerParticipant();
        CountDownLatch lateReady = new CountDownLatch(1);
        CountDownLatch allowLateArrival = new CountDownLatch(1);
        CountDownLatch lateFinishedPhaseOne = new CountDownLatch(1);

        Thread lateThread = new Thread(() -> {
            lateReady.countDown();
            await(allowLateArrival);
            late.arriveAndAwaitAdvance();
            lateFinishedPhaseOne.countDown();
            await(allowDeregistration);
            late.arriveAndDeregister();
        });
        lateThread.start();

        assertTrue(lateReady.await(1, TimeUnit.SECONDS));

        allowInitialArrival.countDown();
        assertFalse(initialFinishedPhaseOne.await(150, TimeUnit.MILLISECONDS));

        allowLateArrival.countDown();

        assertTrue(initialFinishedPhaseOne.await(1, TimeUnit.SECONDS));
        assertTrue(lateFinishedPhaseOne.await(1, TimeUnit.SECONDS));
        assertEquals(2, coordinator.awaitPhaseStart(2));
        assertEquals(2, coordinator.getRegisteredParties());

        allowDeregistration.countDown();

        initialThread.join(1000);
        lateThread.join(1000);
        assertFalse(initialThread.isAlive());
        assertFalse(lateThread.isAlive());
    }

    /**
     * Verifies that deregistering a participant reduces the number of arrivals needed later.
     */
    @Test
    void deregistrationReducesRequiredPartiesForLaterPhases() throws InterruptedException {
        PhasedTaskCoordinator coordinator = new PhasedTaskCoordinator(3);
        PhasedTaskCoordinator.Participant first = coordinator.registerParticipant();
        PhasedTaskCoordinator.Participant second = coordinator.registerParticipant();

        CountDownLatch firstReachedPhaseOne = new CountDownLatch(1);
        CountDownLatch firstCompletedPhaseOne = new CountDownLatch(1);

        Thread firstThread = new Thread(() -> {
            first.arriveAndAwaitAdvance();
            firstReachedPhaseOne.countDown();
            first.arriveAndAwaitAdvance();
            firstCompletedPhaseOne.countDown();
            first.arriveAndDeregister();
        });
        firstThread.start();

        second.arriveAndAwaitAdvance();
        second.arriveAndDeregister();

        assertTrue(firstReachedPhaseOne.await(1, TimeUnit.SECONDS));
        assertTrue(firstCompletedPhaseOne.await(1, TimeUnit.SECONDS));

        firstThread.join(1000);
        assertFalse(firstThread.isAlive());
        assertEquals(0, coordinator.getRegisteredParties());
        assertTrue(coordinator.isTerminated());
    }

    /**
     * Verifies that the coordinator terminates after its configured number of phases.
     */
    @Test
    void terminatesAfterConfiguredNumberOfPhases() {
        PhasedTaskCoordinator coordinator = new PhasedTaskCoordinator(1);
        PhasedTaskCoordinator.Participant participant = coordinator.registerParticipant();

        participant.arriveAndAwaitAdvance();

        assertTrue(coordinator.isTerminated());
        assertThrows(IllegalStateException.class, coordinator::registerParticipant);
    }

    /**
     * Verifies that invalid construction and phase requests are rejected.
     */
    @Test
    void rejectsInvalidArguments() {
        IllegalArgumentException phasesThrown = assertThrows(
                IllegalArgumentException.class,
                () -> new PhasedTaskCoordinator(0)
        );
        assertEquals("totalPhases must be positive", phasesThrown.getMessage());

        PhasedTaskCoordinator coordinator = new PhasedTaskCoordinator(2);
        IllegalArgumentException phaseThrown = assertThrows(
                IllegalArgumentException.class,
                () -> coordinator.awaitPhaseStart(2)
        );
        assertEquals("phase must be between 0 and totalPhases - 1", phaseThrown.getMessage());
    }

    /**
     * Verifies that a participant handle cannot be reused after deregistration.
     */
    @Test
    void rejectsOperationsOnInactiveParticipants() {
        PhasedTaskCoordinator coordinator = new PhasedTaskCoordinator(2);
        PhasedTaskCoordinator.Participant participant = coordinator.registerParticipant();

        participant.arriveAndDeregister();

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                participant::arriveAndAwaitAdvance
        );
        assertEquals("participant is not active", thrown.getMessage());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(1, TimeUnit.SECONDS)) {
                throw new AssertionError("timed out waiting for latch");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("unexpected interruption", e);
        }
    }
}
