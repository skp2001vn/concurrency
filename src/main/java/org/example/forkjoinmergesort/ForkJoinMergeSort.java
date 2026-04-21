package org.example.forkjoinmergesort;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Sorts integer arrays with a fork/join merge sort.
 *
 * <p>The sorter recursively splits the array into disjoint ranges, sorts smaller
 * ranges in parallel via a {@link ForkJoinPool}, and merges the results after
 * child tasks complete. The input array is never mutated; callers receive a
 * sorted copy instead.
 */
public class ForkJoinMergeSort {

    private static final int DEFAULT_SEQUENTIAL_THRESHOLD = 32;

    private final ForkJoinPool forkJoinPool;
    private final int sequentialThreshold;

    /**
     * Creates a sorter that uses the common fork/join pool and a default leaf threshold.
     */
    public ForkJoinMergeSort() {
        this(ForkJoinPool.commonPool(), DEFAULT_SEQUENTIAL_THRESHOLD);
    }

    /**
     * Creates a sorter that uses the common fork/join pool and the provided leaf threshold.
     *
     * @param sequentialThreshold the maximum range size to sort sequentially
     */
    public ForkJoinMergeSort(int sequentialThreshold) {
        this(ForkJoinPool.commonPool(), sequentialThreshold);
    }

    /**
     * Creates a sorter backed by the supplied fork/join pool.
     *
     * @param forkJoinPool the pool that executes recursive sort tasks
     * @param sequentialThreshold the maximum range size to sort sequentially
     */
    public ForkJoinMergeSort(ForkJoinPool forkJoinPool, int sequentialThreshold) {
        this.forkJoinPool = Objects.requireNonNull(forkJoinPool, "forkJoinPool must not be null");
        if (sequentialThreshold <= 0) {
            throw new IllegalArgumentException("sequentialThreshold must be positive");
        }
        this.sequentialThreshold = sequentialThreshold;
    }

    /**
     * Returns a sorted copy of the supplied values in ascending order.
     *
     * @param values the values to sort
     * @return a new array containing the sorted values
     */
    public int[] sort(int[] values) {
        Objects.requireNonNull(values, "values must not be null");

        int[] sorted = Arrays.copyOf(values, values.length);
        if (sorted.length < 2) {
            return sorted;
        }

        int[] workspace = new int[sorted.length];
        forkJoinPool.invoke(new SortTask(sorted, workspace, 0, sorted.length));
        return sorted;
    }

    private final class SortTask extends RecursiveAction {
        private final int[] values;
        private final int[] workspace;
        private final int start;
        private final int end;

        private SortTask(int[] values, int[] workspace, int start, int end) {
            this.values = values;
            this.workspace = workspace;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            int length = end - start;
            if (length <= sequentialThreshold) {
                Arrays.sort(values, start, end);
                return;
            }

            int mid = start + (length / 2);
            invokeAll(
                    new SortTask(values, workspace, start, mid),
                    new SortTask(values, workspace, mid, end)
            );

            if (values[mid - 1] <= values[mid]) {
                return;
            }

            merge(mid);
        }

        private void merge(int mid) {
            System.arraycopy(values, start, workspace, start, end - start);

            int leftIndex = start;
            int rightIndex = mid;
            int destinationIndex = start;

            while (leftIndex < mid && rightIndex < end) {
                if (workspace[leftIndex] <= workspace[rightIndex]) {
                    values[destinationIndex++] = workspace[leftIndex++];
                } else {
                    values[destinationIndex++] = workspace[rightIndex++];
                }
            }

            while (leftIndex < mid) {
                values[destinationIndex++] = workspace[leftIndex++];
            }

            while (rightIndex < end) {
                values[destinationIndex++] = workspace[rightIndex++];
            }
        }
    }
}
