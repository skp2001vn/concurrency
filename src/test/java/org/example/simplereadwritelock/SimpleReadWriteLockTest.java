package org.example.simplereadwritelock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SimpleReadWriteLockTest {

    /**
     * Verifies that multiple readers can hold the read lock concurrently.
     */
    @Test
    void allowsMultipleReadersAtTheSameTime() throws InterruptedException {
        SimpleReadWriteLock lock = new SimpleReadWriteLock();
        CountDownLatch bothReading = new CountDownLatch(2);
        CountDownLatch releaseReaders = new CountDownLatch(1);
        AtomicInteger activeReaders = new AtomicInteger();
        AtomicInteger maxConcurrentReaders = new AtomicInteger();

        Runnable reader = () -> {
            try {
                lock.lockRead();
                int current = activeReaders.incrementAndGet();
                maxConcurrentReaders.accumulateAndGet(current, Math::max);
                bothReading.countDown();
                releaseReaders.await();
                activeReaders.decrementAndGet();
                lock.unlockRead();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Thread r1 = new Thread(reader);
        Thread r2 = new Thread(reader);
        r1.start();
        r2.start();

        assertTrue(bothReading.await(1, TimeUnit.SECONDS));
        releaseReaders.countDown();
        r1.join(1000);
        r2.join(1000);

        assertEquals(2, maxConcurrentReaders.get());
    }

    /**
     * Verifies that readers wait while a writer holds the write lock.
     */
    @Test
    void writerBlocksReadersUntilWriteLockIsReleased() throws InterruptedException {
        SimpleReadWriteLock lock = new SimpleReadWriteLock();
        CountDownLatch writerHolding = new CountDownLatch(1);
        CountDownLatch releaseWriter = new CountDownLatch(1);
        CountDownLatch readerPassed = new CountDownLatch(1);

        Thread writer = new Thread(() -> {
            try {
                lock.lockWrite();
                writerHolding.countDown();
                releaseWriter.await();
                lock.unlockWrite();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread reader = new Thread(() -> {
            try {
                lock.lockRead();
                readerPassed.countDown();
                lock.unlockRead();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        writer.start();
        assertTrue(writerHolding.await(1, TimeUnit.SECONDS));
        reader.start();

        Thread.sleep(100);
        assertEquals(1, readerPassed.getCount());

        releaseWriter.countDown();
        assertTrue(readerPassed.await(1, TimeUnit.SECONDS));
        writer.join(1000);
        reader.join(1000);
        assertFalse(writer.isAlive());
        assertFalse(reader.isAlive());
    }

    /**
     * Verifies that a queued writer prevents newly arriving readers from barging ahead.
     */
    @Test
    void waitingWriterPreventsNewReadersFromEntering() throws InterruptedException {
        SimpleReadWriteLock lock = new SimpleReadWriteLock();
        CountDownLatch firstReaderHolding = new CountDownLatch(1);
        CountDownLatch releaseFirstReader = new CountDownLatch(1);
        CountDownLatch writerWaiting = new CountDownLatch(1);
        CountDownLatch writerAcquired = new CountDownLatch(1);
        CountDownLatch secondReaderPassed = new CountDownLatch(1);

        Thread firstReader = new Thread(() -> {
            try {
                lock.lockRead();
                firstReaderHolding.countDown();
                releaseFirstReader.await();
                lock.unlockRead();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread writer = new Thread(() -> {
            try {
                writerWaiting.countDown();
                lock.lockWrite();
                writerAcquired.countDown();
                Thread.sleep(100);
                lock.unlockWrite();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread secondReader = new Thread(() -> {
            try {
                lock.lockRead();
                secondReaderPassed.countDown();
                lock.unlockRead();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        firstReader.start();
        assertTrue(firstReaderHolding.await(1, TimeUnit.SECONDS));

        writer.start();
        assertTrue(writerWaiting.await(1, TimeUnit.SECONDS));
        Thread.sleep(50);

        secondReader.start();
        Thread.sleep(100);
        assertEquals(1, secondReaderPassed.getCount());

        releaseFirstReader.countDown();
        assertTrue(writerAcquired.await(1, TimeUnit.SECONDS));
        assertTrue(secondReaderPassed.await(1, TimeUnit.SECONDS));

        firstReader.join(1000);
        writer.join(1000);
        secondReader.join(1000);
    }
}
