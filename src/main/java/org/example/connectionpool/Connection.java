package org.example.connectionpool;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Business logic: identifies one reusable connection managed by the connection
 * pool example.
 *
 * <p>Technique: uses Lombok for this immutable data holder because the class has
 * no concurrency behavior of its own. Keeping boilerplate generated lets the
 * example focus synchronization details in {@link ConnectionPool}.
 */
@Getter
@RequiredArgsConstructor
public class Connection {
    private final int id;
}
