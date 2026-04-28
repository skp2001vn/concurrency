package org.example.virtualthreadfetcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.virtualthreadfetcher.VirtualThreadFetcher.FetchResult;
import org.junit.jupiter.api.Test;

class VirtualThreadFetcherTest {

    /**
     * Verifies that later fetches may finish earlier while the returned results still preserve
     * input order.
     */
    @Test
    void fetchesAllResourcesAndPreservesInputOrder() throws Exception {
        CountDownLatch secondCompleted = new CountDownLatch(1);
        List<String> completionOrder = Collections.synchronizedList(new ArrayList<>());
        VirtualThreadFetcher fetcher = new VirtualThreadFetcher(resourceId -> switch (resourceId) {
            case "page-1" -> {
                if (!secondCompleted.await(1, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for page-2 to finish");
                }
                completionOrder.add(resourceId);
                yield "content-1";
            }
            case "page-2" -> {
                completionOrder.add(resourceId);
                secondCompleted.countDown();
                yield "content-2";
            }
            case "page-3" -> {
                completionOrder.add(resourceId);
                yield "content-3";
            }
            default -> throw new IllegalArgumentException(resourceId);
        });

        List<FetchResult> results = fetcher.fetchAll(List.of("page-1", "page-2", "page-3"));

        assertTrue(completionOrder.indexOf("page-2") < completionOrder.indexOf("page-1"));
        assertEquals(
                List.of(
                        new FetchResult("page-1", "content-1"),
                        new FetchResult("page-2", "content-2"),
                        new FetchResult("page-3", "content-3")),
                results);
        assertThrows(
                UnsupportedOperationException.class,
                () -> results.add(new FetchResult("page-4", "content-4")));
    }

    /**
     * Verifies that each submitted blocking fetch runs on a virtual thread and can overlap with
     * the others.
     */
    @Test
    void usesVirtualThreadsForConcurrentFetches() throws InterruptedException {
        CountDownLatch allStarted = new CountDownLatch(3);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger concurrentFetches = new AtomicInteger();
        AtomicInteger maxConcurrentFetches = new AtomicInteger();
        AtomicBoolean allVirtualThreads = new AtomicBoolean(true);

        VirtualThreadFetcher fetcher = new VirtualThreadFetcher(resourceId -> blockUntilReleased(
                resourceId,
                allStarted,
                release,
                concurrentFetches,
                maxConcurrentFetches,
                allVirtualThreads));

        CompletableFuture<List<FetchResult>> future = CompletableFuture.supplyAsync(
                () -> fetchAllUnchecked(fetcher, List.of("page-1", "page-2", "page-3")));

        assertTrue(allStarted.await(1, TimeUnit.SECONDS));
        assertFalse(future.isDone());
        assertEquals(3, maxConcurrentFetches.get());
        assertTrue(allVirtualThreads.get());

        release.countDown();

        assertEquals(
                List.of(
                        new FetchResult("page-1", "content-page-1"),
                        new FetchResult("page-2", "content-page-2"),
                        new FetchResult("page-3", "content-page-3")),
                future.join());
    }

    /**
     * Verifies that a blocking fetch failure is surfaced to the caller while waiting for results.
     */
    @Test
    void propagatesFailuresFromFetchTasks() {
        VirtualThreadFetcher fetcher = new VirtualThreadFetcher(resourceId -> {
            if ("page-2".equals(resourceId)) {
                throw new IOException("upstream timeout");
            }
            return "content-" + resourceId;
        });

        ExecutionException thrown =
                assertThrows(
                        ExecutionException.class,
                        () -> fetcher.fetchAll(List.of("page-1", "page-2", "page-3")));

        assertEquals(IOException.class, thrown.getCause().getClass());
        assertEquals("upstream timeout", thrown.getCause().getMessage());
    }

    /**
     * Verifies that an empty request list returns immediately without invoking the client.
     */
    @Test
    void returnsEmptyListWithoutCallingClient() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        VirtualThreadFetcher fetcher = new VirtualThreadFetcher(resourceId -> {
            calls.incrementAndGet();
            return "content-" + resourceId;
        });

        List<FetchResult> results = fetcher.fetchAll(List.of());

        assertTrue(results.isEmpty());
        assertEquals(0, calls.get());
    }

    private static List<FetchResult> fetchAllUnchecked(
            VirtualThreadFetcher fetcher,
            List<String> resourceIds) {
        try {
            return fetcher.fetchAll(resourceIds);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private String blockUntilReleased(
            String resourceId,
            CountDownLatch allStarted,
            CountDownLatch release,
            AtomicInteger concurrentFetches,
            AtomicInteger maxConcurrentFetches,
            AtomicBoolean allVirtualThreads) {
        if (!Thread.currentThread().isVirtual()) {
            allVirtualThreads.set(false);
        }

        int currentFetches = concurrentFetches.incrementAndGet();
        maxConcurrentFetches.accumulateAndGet(currentFetches, Math::max);
        allStarted.countDown();

        try {
            if (!release.await(1, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release blocked fetches");
            }
            return "content-" + resourceId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            concurrentFetches.decrementAndGet();
        }
    }
}
