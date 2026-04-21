package org.example.forkjoinmergesort;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ForkJoinMergeSortTest {

    /**
     * Verifies that the sorter orders negative values, duplicates, and unsorted input correctly.
     */
    @Test
    void sortsValuesInAscendingOrder() {
        ForkJoinMergeSort sorter = new ForkJoinMergeSort(2);

        int[] sorted = sorter.sort(new int[] {7, -3, 5, 5, 0, 9, -1, 2});

        assertArrayEquals(new int[] {-3, -1, 0, 2, 5, 5, 7, 9}, sorted);
    }

    /**
     * Verifies that sorting returns a new array and leaves the caller's input untouched.
     */
    @Test
    void returnsSortedCopyWithoutMutatingInput() {
        ForkJoinMergeSort sorter = new ForkJoinMergeSort(2);
        int[] input = {4, 1, 3, 2};

        int[] sorted = sorter.sort(input);

        assertNotSame(input, sorted);
        assertArrayEquals(new int[] {4, 1, 3, 2}, input);
        assertArrayEquals(new int[] {1, 2, 3, 4}, sorted);
    }

    /**
     * Verifies that empty and single-element arrays are handled without extra work.
     */
    @Test
    void handlesEmptyAndSingleElementInputs() {
        ForkJoinMergeSort sorter = new ForkJoinMergeSort();

        assertArrayEquals(new int[0], sorter.sort(new int[0]));
        assertArrayEquals(new int[] {42}, sorter.sort(new int[] {42}));
    }

    /**
     * Verifies that invalid constructor arguments are rejected with clear failures.
     */
    @Test
    void rejectsInvalidConstructorArguments() {
        IllegalArgumentException thresholdThrown = assertThrows(
                IllegalArgumentException.class,
                () -> new ForkJoinMergeSort(0)
        );
        assertEquals("sequentialThreshold must be positive", thresholdThrown.getMessage());

        NullPointerException poolThrown = assertThrows(
                NullPointerException.class,
                () -> new ForkJoinMergeSort(null, 4)
        );
        assertEquals("forkJoinPool must not be null", poolThrown.getMessage());
    }

    /**
     * Verifies that null input arrays are rejected.
     */
    @Test
    void rejectsNullInput() {
        ForkJoinMergeSort sorter = new ForkJoinMergeSort();

        NullPointerException thrown = assertThrows(
                NullPointerException.class,
                () -> sorter.sort(null)
        );

        assertEquals("values must not be null", thrown.getMessage());
    }

    /**
     * Verifies that one sorter instance can serve multiple concurrent callers safely.
     */
    @Test
    void supportsConcurrentSortCallsOnTheSameSorter() throws Exception {
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        ExecutorService callers = Executors.newFixedThreadPool(4);

        try {
            ForkJoinMergeSort sorter = new ForkJoinMergeSort(forkJoinPool, 2);
            int[][] inputs = {
                    {9, 1, 8, 2, 7, 3, 6, 4, 5},
                    {5, 4, 3, 2, 1},
                    {10, -5, 0, 10, 3, -5},
                    {2, 2, 2, 1, 1, 3}
            };
            int[][] expected = {
                    {1, 2, 3, 4, 5, 6, 7, 8, 9},
                    {1, 2, 3, 4, 5},
                    {-5, -5, 0, 3, 10, 10},
                    {1, 1, 2, 2, 2, 3}
            };

            CountDownLatch ready = new CountDownLatch(inputs.length);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<int[]>> futures = new ArrayList<>();

            for (int[] input : inputs) {
                futures.add(callers.submit(() -> {
                    ready.countDown();
                    boolean started = start.await(1, TimeUnit.SECONDS);
                    assertTrue(started);
                    return sorter.sort(input);
                }));
            }

            assertTrue(ready.await(1, TimeUnit.SECONDS));
            start.countDown();

            for (int i = 0; i < futures.size(); i++) {
                assertArrayEquals(expected[i], futures.get(i).get());
            }
        } finally {
            callers.shutdownNow();
            forkJoinPool.shutdownNow();
        }
    }
}
