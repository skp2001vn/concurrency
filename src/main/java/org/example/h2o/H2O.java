package org.example.h2o;

import java.util.concurrent.Semaphore;

/**
 * Business logic: groups hydrogen and oxygen workers into valid water molecules
 * with exactly two hydrogen callbacks and one oxygen callback per group.
 *
 * <p>Technique: uses semaphores because the molecule ratio is naturally modeled
 * as permits. Two hydrogen releases enable one oxygen release, and oxygen
 * restores permits for the next molecule without needing a global lock.
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
