package org.example.webcrawler;

import java.util.List;

/**
 * Business logic: abstracts page link extraction for the web crawler so tests
 * and callers can provide their own page source.
 *
 * <p>Technique: keeps network or parsing behavior behind a small interface
 * because the crawler's concurrency can then be tested with deterministic fakes.
 * This separates I/O concerns from queueing, deduplication, and termination
 * logic in {@link WebCrawler}.
 */
public interface HtmlParser {

    /**
     * Returns all URLs found on the page identified by {@code url}.
     *
     * @param url the page to inspect
     * @return the URLs reachable from the provided page
     */
    List<String> getUrls(String url);
}
