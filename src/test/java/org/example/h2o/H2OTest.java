package org.example.h2o;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

class H2OTest {

    @Test
    void formsSingleWaterMolecule() throws InterruptedException {
        List<String> output = runH2O(2, 1);

        assertEquals(3, output.size());
        assertEquals(2, output.stream().filter("H"::equals).count());
        assertEquals(1, output.stream().filter("O"::equals).count());
        assertEquals(List.of("H", "H", "O"), output);
    }

    @Test
    void formsMultipleWaterMoleculesInValidGroups() throws InterruptedException {
        List<String> output = runH2O(6, 3);

        assertEquals(9, output.size());
        for (int i = 0; i < output.size(); i += 3) {
            List<String> molecule = output.subList(i, i + 3);
            assertEquals(List.of("H", "H", "O"), molecule);
        }
    }

    @Test
    void oxygenWaitsUntilTwoHydrogensArrive() throws InterruptedException {
        H2O h2o = new H2O();
        List<String> output = new CopyOnWriteArrayList<>();

        Thread oxygen = new Thread(() -> runUnchecked(() -> h2o.oxygen(() -> output.add("O"))));
        oxygen.start();

        Thread.sleep(100);
        assertTrueAlive(oxygen);

        Thread h1 = new Thread(() -> runUnchecked(() -> h2o.hydrogen(() -> output.add("H"))));
        Thread h2 = new Thread(() -> runUnchecked(() -> h2o.hydrogen(() -> output.add("H"))));
        h1.start();
        h2.start();

        oxygen.join(1000);
        h1.join(1000);
        h2.join(1000);

        assertFalse(oxygen.isAlive());
        assertFalse(h1.isAlive());
        assertFalse(h2.isAlive());
        assertEquals(List.of("H", "H", "O"), new ArrayList<>(output));
    }

    private List<String> runH2O(int hydrogenCount, int oxygenCount) throws InterruptedException {
        H2O h2o = new H2O();
        List<String> output = new CopyOnWriteArrayList<>();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < oxygenCount; i++) {
            threads.add(new Thread(() -> runUnchecked(() -> h2o.oxygen(() -> output.add("O")))));
        }
        for (int i = 0; i < hydrogenCount; i++) {
            threads.add(new Thread(() -> runUnchecked(() -> h2o.hydrogen(() -> output.add("H")))));
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join(1000);
            assertFalse(thread.isAlive());
        }

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

    private void assertTrueAlive(Thread thread) {
        if (!thread.isAlive()) {
            throw new AssertionError("Expected thread to still be waiting");
        }
    }
}
