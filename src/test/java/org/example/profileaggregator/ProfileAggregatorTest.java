package org.example.profileaggregator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.profileaggregator.ProfileAggregator.Order;
import org.example.profileaggregator.ProfileAggregator.ProfileSnapshot;
import org.example.profileaggregator.ProfileAggregator.Recommendation;
import org.example.profileaggregator.ProfileAggregator.UserProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProfileAggregatorTest {

    private ExecutorService executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * Verifies that the aggregator combines all three service results into one snapshot.
     */
    @Test
    void aggregatesProfileDataFromAllServices() {
        executor = Executors.newFixedThreadPool(3);
        ProfileAggregator aggregator = new ProfileAggregator(
                userId -> new UserProfile(userId, "Ada Lovelace", "ada@example.com"),
                userId -> List.of(
                        new Order("order-1", "Mechanical keyboard"),
                        new Order("order-2", "Standing desk")),
                userId -> List.of(
                        new Recommendation("item-1", "Based on recent purchases"),
                        new Recommendation("item-2", "Popular with similar buyers")),
                executor);

        ProfileSnapshot snapshot = aggregator.aggregate("user-1").join();

        assertEquals(
                new ProfileSnapshot(
                        new UserProfile("user-1", "Ada Lovelace", "ada@example.com"),
                        List.of(
                                new Order("order-1", "Mechanical keyboard"),
                                new Order("order-2", "Standing desk")),
                        List.of(
                                new Recommendation(
                                        "item-1",
                                        "Based on recent purchases"),
                                new Recommendation(
                                        "item-2",
                                        "Popular with similar buyers"))),
                snapshot);
    }

    /**
     * Verifies that the three independent lookups are started in parallel before combination.
     */
    @Test
    void startsIndependentLookupsInParallel() throws InterruptedException {
        executor = Executors.newFixedThreadPool(3);
        CountDownLatch allStarted = new CountDownLatch(3);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger concurrentCalls = new AtomicInteger();
        AtomicInteger maxConcurrentCalls = new AtomicInteger();

        ProfileAggregator aggregator = new ProfileAggregator(
                userId -> blockUntilReleased(
                        allStarted,
                        release,
                        concurrentCalls,
                        maxConcurrentCalls,
                        new UserProfile(userId, "Ada Lovelace", "ada@example.com")),
                userId -> blockUntilReleased(
                        allStarted,
                        release,
                        concurrentCalls,
                        maxConcurrentCalls,
                        List.of(new Order("order-1", "Mechanical keyboard"))),
                userId -> blockUntilReleased(
                        allStarted,
                        release,
                        concurrentCalls,
                        maxConcurrentCalls,
                        List.of(new Recommendation("item-1", "Based on recent purchases"))),
                executor);

        CompletableFuture<ProfileSnapshot> future = aggregator.aggregate("user-1");

        assertTrue(allStarted.await(1, TimeUnit.SECONDS));
        assertFalse(future.isDone());
        assertEquals(3, maxConcurrentCalls.get());

        release.countDown();

        assertEquals("user-1", future.join().user().userId());
    }

    /**
     * Verifies that a failure in any source causes the aggregated future to fail.
     */
    @Test
    void completesExceptionallyWhenAnyLookupFails() {
        executor = Executors.newFixedThreadPool(3);
        ProfileAggregator aggregator = new ProfileAggregator(
                userId -> new UserProfile(userId, "Ada Lovelace", "ada@example.com"),
                userId -> {
                    throw new IllegalStateException("order service unavailable");
                },
                userId -> List.of(new Recommendation("item-1", "Based on recent purchases")),
                executor);

        CompletionException thrown =
                assertThrows(CompletionException.class, () -> aggregator.aggregate("user-1").join());

        assertEquals(IllegalStateException.class, thrown.getCause().getClass());
        assertEquals("order service unavailable", thrown.getCause().getMessage());
    }

    /**
     * Verifies that empty source results are preserved and exposed through immutable lists.
     */
    @Test
    void preservesEmptyResultsWithImmutableSnapshots() {
        executor = Executors.newFixedThreadPool(3);
        List<Order> orders = new ArrayList<>();
        List<Recommendation> recommendations = new ArrayList<>();
        ProfileAggregator aggregator = new ProfileAggregator(
                userId -> new UserProfile(userId, "Ada Lovelace", "ada@example.com"),
                userId -> orders,
                userId -> recommendations,
                executor);

        ProfileSnapshot snapshot = aggregator.aggregate("user-1").join();
        orders.add(new Order("order-2", "Desk lamp"));
        recommendations.add(new Recommendation("item-2", "Recently viewed"));

        assertTrue(snapshot.recentOrders().isEmpty());
        assertTrue(snapshot.recommendations().isEmpty());
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.recentOrders().add(new Order("order-3", "Mouse")));
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.recommendations().add(
                        new Recommendation("item-3", "Top seller")));
    }

    private <T> T blockUntilReleased(
            CountDownLatch allStarted,
            CountDownLatch release,
            AtomicInteger concurrentCalls,
            AtomicInteger maxConcurrentCalls,
            T result) {
        int currentCalls = concurrentCalls.incrementAndGet();
        maxConcurrentCalls.accumulateAndGet(currentCalls, Math::max);
        allStarted.countDown();

        try {
            if (!release.await(1, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release blocked lookup");
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            concurrentCalls.decrementAndGet();
        }
    }
}
