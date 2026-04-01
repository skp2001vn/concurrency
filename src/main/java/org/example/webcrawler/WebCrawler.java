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
 * Crawls pages concurrently starting from a seed URL and returns all reachable URLs that share the
 * same hostname.
 *
 * <p>The crawler uses a fixed-size worker pool and a shared blocking queue of discovered URLs.
 * URLs are deduplicated with a concurrent visited set, and an in-flight task counter prevents
 * workers from exiting early while other threads are still discovering more pages.
 */
public class WebCrawler {
    private static final int WORKER_COUNT = 8;

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
