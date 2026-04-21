package org.example.webcrawler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WebCrawlerTest {

    /**
     * Verifies that crawling stays within the starting hostname.
     */
    @Test
    void crawlsOnlyUrlsUnderTheSameHostname() {
        FakeHtmlParser parser = new FakeHtmlParser(Map.of(
                "http://news.example.com", List.of(
                        "http://news.example.com/world",
                        "http://other.example.com/about",
                        "http://news.example.com/sports"),
                "http://news.example.com/world", List.of("http://news.example.com/archive"),
                "http://news.example.com/sports", List.of(),
                "http://news.example.com/archive", List.of(),
                "http://other.example.com/about", List.of("http://news.example.com/ignored")));

        List<String> crawled = new WebCrawler().crawl("http://news.example.com", parser);

        assertEquals(
                List.of(
                        "http://news.example.com",
                        "http://news.example.com/archive",
                        "http://news.example.com/sports",
                        "http://news.example.com/world"),
                sort(crawled));
    }

    /**
     * Verifies that the crawler deduplicates URLs reached from multiple pages.
     */
    @Test
    void doesNotRevisitDuplicateUrlsReachedFromMultiplePages() {
        FakeHtmlParser parser = new FakeHtmlParser(Map.of(
                "http://example.org", List.of(
                        "http://example.org/a",
                        "http://example.org/b"),
                "http://example.org/a", List.of("http://example.org/shared"),
                "http://example.org/b", List.of("http://example.org/shared"),
                "http://example.org/shared", List.of()));

        List<String> crawled = new WebCrawler().crawl("http://example.org", parser);

        assertEquals(
                List.of(
                        "http://example.org",
                        "http://example.org/a",
                        "http://example.org/b",
                        "http://example.org/shared"),
                sort(crawled));
        assertEquals(1, parser.getInvocationCount("http://example.org/shared"));
    }

    /**
     * Verifies that cyclic link graphs are handled without infinite crawling.
     */
    @Test
    void handlesCyclesWithoutLoopingForever() {
        FakeHtmlParser parser = new FakeHtmlParser(Map.of(
                "http://site.com", List.of("http://site.com/a"),
                "http://site.com/a", List.of("http://site.com/b"),
                "http://site.com/b", List.of("http://site.com")));

        List<String> crawled = new WebCrawler().crawl("http://site.com", parser);

        assertEquals(
                List.of(
                        "http://site.com",
                        "http://site.com/a",
                        "http://site.com/b"),
                sort(crawled));
    }

    /**
     * Verifies that crawling continues even after workers temporarily observe an empty queue.
     */
    @Test
    void continuesCrawlingWhenMoreUrlsAppearAfterATemporaryEmptyQueue() {
        BlockingFakeHtmlParser parser = new BlockingFakeHtmlParser(Map.of(
                "http://site.com", List.of("http://site.com/a"),
                "http://site.com/a", List.of("http://site.com/b"),
                "http://site.com/b", List.of()));

        List<String> crawled = new WebCrawler().crawl("http://site.com", parser);

        assertEquals(
                List.of(
                        "http://site.com",
                        "http://site.com/a",
                        "http://site.com/b"),
                sort(crawled));
    }

    private List<String> sort(List<String> urls) {
        List<String> sorted = new ArrayList<>(urls);
        sorted.sort(String::compareTo);
        return sorted;
    }

    private static class FakeHtmlParser implements HtmlParser {
        private final Map<String, List<String>> graph;
        private final Map<String, AtomicInteger> invocations = new ConcurrentHashMap<>();

        private FakeHtmlParser(Map<String, List<String>> graph) {
            this.graph = new HashMap<>(graph);
        }

        @Override
        public List<String> getUrls(String url) {
            invocations.computeIfAbsent(url, ignored -> new AtomicInteger()).incrementAndGet();
            return graph.getOrDefault(url, List.of());
        }

        private int getInvocationCount(String url) {
            AtomicInteger count = invocations.get(url);
            return count == null ? 0 : count.get();
        }
    }

    private static final class BlockingFakeHtmlParser extends FakeHtmlParser {
        private final CountDownLatch startVisited = new CountDownLatch(1);

        private BlockingFakeHtmlParser(Map<String, List<String>> graph) {
            super(graph);
        }

        @Override
        public List<String> getUrls(String url) {
            if ("http://site.com".equals(url)) {
                startVisited.countDown();
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                try {
                    startVisited.await(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            return super.getUrls(url);
        }
    }
}
