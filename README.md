# Concurrency

A Java 17 Maven project that implements and tests a set of concurrency patterns, synchronization primitives, and thread-safe data structures.

The codebase is organized as small, focused introductory examples. Each example demonstrates a concurrency technique and is covered by JUnit 5 tests.

## Requirements

- Java 17+
- Maven 3.9+

## Build And Test

```bash
mvn test
```

The current test suite contains 50 passing tests across all examples.

## Implemented Examples

| Example | What it demonstrates |
| --- | --- |
| `boundedbuffer` | Producer-consumer coordination with `ReentrantLock` and `Condition`s |
| `simplethreadpool` | Fixed-size thread pool with task queue and worker threads |
| `taskscheduler` | Delayed task execution with a priority queue and worker thread |
| `simplecyclicbarrier` | Reusable barrier that releases threads together by generation |
| `simplereadwritelock` | Basic reader-writer lock with writer preference |
| `connectionpool` | Connection acquisition and release with timeout, fairness, and wait limits |
| `memoizer` | Shared computation results via `Future` and `FutureTask` |
| `expiringcache` | TTL cache with background cleanup |
| `expiringlrucache` | Cache with TTL and least-recently-used eviction |
| `inventory` | Two thread-safe inventory implementations: lock-based and atomic/CAS |
| `ratelimiter` | Sliding-window rate limiting with synchronized and lock-based variants |
| `asyncjobqueue` | Asynchronous job queue with retries and dead-letter queue |
| `advancedjobqueue` | Priority and scheduled job execution with retries, cancellation, and dead-letter queue |

## Project Structure

```text
src/main/java/org/example/
src/test/java/org/example/
```

Production code lives under `src/main/java`, and each example has a matching test class under `src/test/java`.
