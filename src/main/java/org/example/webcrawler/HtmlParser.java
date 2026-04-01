package org.example.webcrawler;

import java.util.List;

/**
 * Abstraction over the page-fetching API used by the multithreaded web crawler example.
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
