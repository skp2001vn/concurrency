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
| [`boundedbuffer`](src/main/java/org/example/boundedbuffer/) | Producer-consumer coordination with `ReentrantLock` and `Condition`s |
| [`simplethreadpool`](src/main/java/org/example/simplethreadpool/) | Fixed-size thread pool with task queue and worker threads |
| [`forkjoinmergesort`](src/main/java/org/example/forkjoinmergesort/) | Divide-and-conquer sorting with `ForkJoinPool`, recursive tasks, and parallel merges |
| [`exchangerexample`](src/main/java/org/example/exchangerexample/) | Two-thread rendezvous and value exchange with `Exchanger` |
| [`taskscheduler`](src/main/java/org/example/taskscheduler/) | Delayed task execution with a priority queue and worker thread |
| [`simplecyclicbarrier`](src/main/java/org/example/simplecyclicbarrier/) | Reusable barrier that releases threads together by generation |
| [`phaserexample`](src/main/java/org/example/phaserexample/) | Multi-phase coordination with `Phaser`, including late registration and deregistration |
| [`simplereadwritelock`](src/main/java/org/example/simplereadwritelock/) | Basic reader-writer lock with writer preference |
| [`connectionpool`](src/main/java/org/example/connectionpool/) | Connection acquisition and release with timeout, fairness, and wait limits |
| [`memoizer`](src/main/java/org/example/memoizer/) | Shared computation results via `Future` and `FutureTask` |
| [`parallelreport`](src/main/java/org/example/parallelreport/) | One-shot report section fan-out/fan-in with `CountDownLatch` and partial-failure collection |
| [`profileaggregator`](src/main/java/org/example/profileaggregator/) | Parallel profile fan-out/fan-in with `CompletableFuture` and immutable aggregated results |
| [`virtualthreadfetcher`](src/main/java/org/example/virtualthreadfetcher/) | Blocking bulk fetches with one virtual thread per resource and ordered fan-in |
| [`expiringcache`](src/main/java/org/example/expiringcache/) | TTL cache with background cleanup |
| [`expiringlrucache`](src/main/java/org/example/expiringlrucache/) | Cache with TTL and least-recently-used eviction |
| [`simplecountdownlatch`](src/main/java/org/example/simplecountdownlatch/) | One-shot coordination that releases waiters after a fixed number of countdowns |
| [`fizzbuzz`](src/main/java/org/example/fizzbuzz/) | Semaphore-based coordination of four threads to produce the FizzBuzz sequence in order |
| [`h2o`](src/main/java/org/example/h2o/) | Semaphore-based coordination that groups threads into valid water molecules (`HHO`) |
| [`diningphilosophers`](src/main/java/org/example/diningphilosophers/) | Deadlock-free fork coordination that still allows non-neighboring philosophers to eat concurrently |
| [`inventory`](src/main/java/org/example/inventory/) | Two thread-safe inventory implementations: lock-based and atomic/CAS |
| [`ratelimiter`](src/main/java/org/example/ratelimiter/) | Sliding-window rate limiting with synchronized and lock-based variants |
| [`asyncjobqueue`](src/main/java/org/example/asyncjobqueue/) | Asynchronous job queue with retries and dead-letter queue |
| [`advancedjobqueue`](src/main/java/org/example/advancedjobqueue/) | Priority and scheduled job execution with retries, cancellation, and dead-letter queue |
| [`stampedaccount`](src/main/java/org/example/stampedaccount/) | Bank account guarded by `StampedLock` with optimistic reads and consistent snapshots |
| [`webcrawler`](src/main/java/org/example/webcrawler/) | Concurrent crawling of same-host URLs with a thread pool, deduplication, and cycle handling |

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
