package org.example.webcrawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Business logic: crawls every reachable URL on the same host as a starting URL
 * while ignoring off-host links and duplicate pages.
 *
 * <p>Technique: runs a fixed worker pool over a shared {@link BlockingQueue}
 * because crawling is naturally many independent fetch/parse tasks. A concurrent
 * visited set prevents duplicate work, and an {@link AtomicInteger} in-flight
 * counter lets workers terminate only after all discovered work is finished.
 */
public class WebCrawler {
    private static final int WORKER_COUNT = 8;

    /**
     * Creates a crawler with the default worker count.
     */
    public WebCrawler() {
    }

    /**
     * Crawls the connected same-host subgraph reachable from the starting URL.
     *
     * @param startUrl the initial page to crawl
     * @param htmlParser parser used to fetch links from each page
     * @return all reachable URLs on the same host, in arbitrary order
     */
    public List<String> crawl(String startUrl, HtmlParser htmlParser) {
        String hostname = extractHostname(startUrl);
        Set<String> visited = ConcurrentHashMap.newKeySet();
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        AtomicInteger inFlightUrls = new AtomicInteger(1);

        visited.add(startUrl);
        queue.offer(startUrl);

        ExecutorService executor = Executors.newFixedThreadPool(WORKER_COUNT);
        for (int i = 0; i < WORKER_COUNT; i++) {
            executor.execute(() -> crawlWorker(hostname, htmlParser, visited, queue, inFlightUrls));
        }

        try {
            executor.shutdown();
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return new ArrayList<>(visited);
    }

    private void crawlWorker(
            String hostname,
            HtmlParser htmlParser,
            Set<String> visited,
            BlockingQueue<String> queue,
            AtomicInteger inFlightUrls) {
        while (true) {
            String url = queue.poll();
            if (url == null) {
                if (inFlightUrls.get() == 0) {
                    return;
                }
                Thread.yield();
                continue;
            }

            try {
                for (String nextUrl : htmlParser.getUrls(url)) {
                    if (!extractHostname(nextUrl).equals(hostname)) {
                        continue;
                    }
                    if (visited.add(nextUrl)) {
                        inFlightUrls.incrementAndGet();
                        queue.offer(nextUrl);
                    }
                }
            } finally {
                inFlightUrls.decrementAndGet();
            }
        }
    }

    private String extractHostname(String url) {
        return url.split("/")[2];
    }
}
