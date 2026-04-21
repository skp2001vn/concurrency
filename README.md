# Concurrency Examples

A Java 21 Maven project that implements and tests a set of concurrency patterns, synchronization primitives, and thread-safe data structures.

The codebase is organized as small, focused introductory examples such as thread pools, rate limiters, caches, connection pools,  barriers, read-write locks, bounded buffers, job queues. Each example demonstrates a concurrency technique and is covered by JUnit 5 tests.

The repository also includes an [AGENTS.md](AGENTS.md) guide to keep AI-assisted and human contributions consistent across examples, tests, and documentation.

## Requirements

- Java 21+
- Maven 3.9+

## Build And Test

```bash
mvn test
```

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
| `simplecountdownlatch` | One-shot coordination that releases waiters after a fixed number of countdowns |
| `fizzbuzz` | Semaphore-based coordination of four threads to produce the FizzBuzz sequence in order |
| `h2o` | Semaphore-based coordination that groups threads into valid water molecules (`HHO`) |
| `diningphilosophers` | Deadlock-free fork coordination that still allows non-neighboring philosophers to eat concurrently |
| `inventory` | Two thread-safe inventory implementations: lock-based and atomic/CAS |
| `ratelimiter` | Sliding-window rate limiting with synchronized and lock-based variants |
| `asyncjobqueue` | Asynchronous job queue with retries and dead-letter queue |
| `advancedjobqueue` | Priority and scheduled job execution with retries, cancellation, and dead-letter queue |
| `stampedaccount` | Bank account guarded by `StampedLock` with optimistic reads and consistent snapshots |
| `webcrawler` | Concurrent crawling of same-host URLs with a thread pool, deduplication, and cycle handling |

## Agent Workflow

This repository includes [AGENTS.md](AGENTS.md) to guide AI-assisted and human contributions when adding or updating examples.

Its purpose is to keep the repository consistent as it grows by defining:
- how new examples should be structured
- expectations for Javadoc
- testing requirements
- README maintenance rules
- general code quality and naming conventions

If you add a new example, check `AGENTS.md` before making changes.

## Project Structure

```text
src/main/java/org/example/
src/test/java/org/example/
```

Production code lives under `src/main/java`, and each example has a matching test class under `src/test/java`.
