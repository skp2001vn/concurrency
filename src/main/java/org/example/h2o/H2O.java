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
    private Semaphore hydrogen = new Semaphore(2);
    private Semaphore oxygen = new Semaphore(0);

    public H2O() {}

    public void hydrogen(Runnable releaseHydrogen) throws InterruptedException {

        hydrogen.acquire();
        releaseHydrogen.run();
        oxygen.release();
    }

    public void oxygen(Runnable releaseOxygen) throws InterruptedException {

        oxygen.acquire(2);
        releaseOxygen.run();
        hydrogen.release(2);
    }
}
