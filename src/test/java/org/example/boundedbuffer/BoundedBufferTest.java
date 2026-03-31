package org.example.boundedbuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class BoundedBufferTest {

    @Test
    void consumesItemsInFifoOrder() throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(2);

        buffer.produce(1);
        buffer.produce(2);

        assertEquals(1, buffer.consume());
        assertEquals(2, buffer.consume());
    }

    @Test
    void consumerWaitsUntilAnItemIsProduced() throws InterruptedException {
        BoundedBuffer<String> buffer = new BoundedBuffer<>(1);
        CountDownLatch consumerStarted = new CountDownLatch(1);
        AtomicReference<String> consumed = new AtomicReference<>();

        Thread consumer = new Thread(() -> {
            consumerStarted.countDown();
            try {
                consumed.set(buffer.consume());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumer.start();

        assertTrue(consumerStarted.await(1, TimeUnit.SECONDS));
        Thread.sleep(100);
        assertTrue(consumer.isAlive());
        assertEquals(null, consumed.get());

        buffer.produce("item");
        consumer.join(1000);

        assertFalse(consumer.isAlive());
        assertEquals("item", consumed.get());
    }

    @Test
    void producerWaitsUntilSpaceBecomesAvailable() throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(1);
        CountDownLatch producerStarted = new CountDownLatch(1);
        CountDownLatch producerFinished = new CountDownLatch(1);

        buffer.produce(1);

        Thread producer = new Thread(() -> {
            producerStarted.countDown();
            try {
                buffer.produce(2);
                producerFinished.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producer.start();

        assertTrue(producerStarted.await(1, TimeUnit.SECONDS));
        Thread.sleep(100);
        assertTrue(producer.isAlive());
        assertEquals(1, buffer.consume());

        assertTrue(producerFinished.await(1, TimeUnit.SECONDS));
        assertEquals(2, buffer.consume());
    }
}
