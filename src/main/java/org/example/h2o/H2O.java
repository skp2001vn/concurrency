package org.example.h2o;

import java.util.concurrent.Semaphore;

/**
 * Coordinates hydrogen and oxygen threads so they can form water molecules in groups of two
 * hydrogens and one oxygen.
 *
 * <p>The implementation uses semaphores to allow up to two hydrogen threads to proceed before one
 * oxygen thread is released. After oxygen runs, permits are restored for the next molecule.
 */
public class H2O {
    private final Semaphore hydrogen = new Semaphore(2);
    private final Semaphore oxygen = new Semaphore(0);

    /**
     * Creates a new coordinator for forming water molecules.
     */
    public H2O() {}

    /**
     * Releases one hydrogen atom for the current molecule.
     *
     * @param releaseHydrogen callback used to emit hydrogen output
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void hydrogen(Runnable releaseHydrogen) throws InterruptedException {

        hydrogen.acquire();
        releaseHydrogen.run();
        oxygen.release();
    }

    /**
     * Releases one oxygen atom after two hydrogens have been released.
     *
     * @param releaseOxygen callback used to emit oxygen output
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void oxygen(Runnable releaseOxygen) throws InterruptedException {

        oxygen.acquire(2);
        releaseOxygen.run();
        hydrogen.release(2);
    }
}
