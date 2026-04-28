package org.example.virtualthreadfetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Fetches many blocking resources concurrently by representing each fetch task as its own virtual
 * thread.
 *
 * <p>The fetcher uses {@link Executors#newVirtualThreadPerTaskExecutor()} so callers can keep a
 * straightforward synchronous, blocking fetch API while still scaling to large numbers of
 * concurrent I/O-bound tasks. Results are collected in the same order as the requested resources.
 */
public class VirtualThreadFetcher {

    private final FetchClient fetchClient;

    /**
     * Creates a fetcher that delegates each blocking fetch to the supplied client.
     *
     * @param fetchClient client used to perform the blocking resource fetch
     */
    public VirtualThreadFetcher(FetchClient fetchClient) {
        this.fetchClient = Objects.requireNonNull(fetchClient, "fetchClient");
    }

    /**
     * Fetches all requested resources concurrently using one virtual thread per task.
     *
     * @param resourceIds identifiers or URLs to fetch
     * @return immutable fetch results in the same order as {@code resourceIds}
     * @throws ExecutionException if any fetch fails
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    public List<FetchResult> fetchAll(List<String> resourceIds)
            throws ExecutionException, InterruptedException {
        Objects.requireNonNull(resourceIds, "resourceIds");

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<FetchResult>> futures = new ArrayList<>(resourceIds.size());
            for (String resourceId : resourceIds) {
                String requestedResourceId = Objects.requireNonNull(resourceId, "resourceId");
                futures.add(executor.submit(() ->
                        new FetchResult(requestedResourceId, fetchClient.fetch(requestedResourceId))));
            }

            List<FetchResult> results = new ArrayList<>(futures.size());
            for (Future<FetchResult> future : futures) {
                results.add(future.get());
            }
            return List.copyOf(results);
        }
    }

    /**
     * Performs one blocking resource fetch.
     */
    @FunctionalInterface
    public interface FetchClient {

        /**
         * Returns the fetched content for a resource.
         *
         * @param resourceId identifier or URL to fetch
         * @return fetched content
         * @throws Exception if the fetch fails
         */
        String fetch(String resourceId) throws Exception;
    }

    /**
     * Immutable fetched resource payload.
     *
     * @param resourceId identifier or URL that was fetched
     * @param content fetched content for the resource
     */
    public record FetchResult(String resourceId, String content) {

        public FetchResult {
            resourceId = Objects.requireNonNull(resourceId, "resourceId");
            content = Objects.requireNonNull(content, "content");
        }
    }
}
