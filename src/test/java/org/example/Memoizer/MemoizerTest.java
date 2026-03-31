package org.example.Memoizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MemoizerTest {

    @Test
    void reusesComputedValueForSameKey() throws Exception {
        Memoizer<String, String> memoizer = new Memoizer<>();
        AtomicInteger calls = new AtomicInteger();

        String first = memoizer.compute("A", () -> {
            calls.incrementAndGet();
            return "value";
        });
        String second = memoizer.compute("A", () -> {
            calls.incrementAndGet();
            return "other";
        });

        assertEquals("value", first);
        assertEquals("value", second);
        assertEquals(1, calls.get());
    }

    @Test
    void concurrentRequestsShareSingleComputation() throws Exception {
        Memoizer<String, String> memoizer = new Memoizer<>();
        AtomicInteger calls = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(6);

        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                tasks.add(() -> memoizer.compute("A", () -> {
                    calls.incrementAndGet();
                    Thread.sleep(100);
                    return "shared";
                }));
            }

            List<Future<String>> futures = executor.invokeAll(tasks);
            for (Future<String> future : futures) {
                assertEquals("shared", future.get());
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, calls.get());
    }

    @Test
    void failedComputationIsCachedAsFailedFuture() throws Exception {
        Memoizer<String, String> memoizer = new Memoizer<>();
        AtomicInteger calls = new AtomicInteger();

        ExecutionException first = assertThrows(ExecutionException.class, () ->
                memoizer.compute("A", () -> {
                    calls.incrementAndGet();
                    throw new IllegalStateException("boom");
                }));

        ExecutionException second = assertThrows(ExecutionException.class, () ->
                memoizer.compute("A", () -> {
                    calls.incrementAndGet();
                    return "value";
                }));

        assertTrue(first.getCause() instanceof IllegalStateException);
        assertTrue(second.getCause() instanceof IllegalStateException);
        assertEquals("boom", first.getCause().getMessage());
        assertEquals(1, calls.get());
    }
}
