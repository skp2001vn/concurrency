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
| [`boundedbuffer`](src/main/java/org/example/boundedbuffer/) | Handing off work between producers and consumers without exceeding capacity, using `ReentrantLock` and `Condition`. |
| [`simplethreadpool`](src/main/java/org/example/simplethreadpool/) | Running submitted tasks on a reusable fixed worker set, using `ReentrantLock` and `Condition`. |
| [`forkjoinmergesort`](src/main/java/org/example/forkjoinmergesort/) | Sorting large arrays without mutating caller input, using `ForkJoinPool` and `RecursiveAction`. |
| [`exchangerexample`](src/main/java/org/example/exchangerexample/) | Letting two parties rendezvous and swap payloads, using `Exchanger`. |
| [`taskscheduler`](src/main/java/org/example/taskscheduler/) | Running tasks after their scheduled delay, using `PriorityQueue`, `ReentrantLock`, and `Condition`. |
| [`simplecyclicbarrier`](src/main/java/org/example/simplecyclicbarrier/) | Holding a fixed group at a reusable checkpoint, using `ReentrantLock` and `Condition`. |
| [`phaserexample`](src/main/java/org/example/phaserexample/) | Coordinating multi-phase work where participants can join or leave, using `Phaser`. |
| [`simplereadwritelock`](src/main/java/org/example/simplereadwritelock/) | Protecting shared data with concurrent reads and exclusive writes, using `ReentrantLock` and `Condition`. |
| [`connectionpool`](src/main/java/org/example/connectionpool/) | Sharing a limited pool of reusable connections with timeouts, using fair `ReentrantLock` and `Condition`. |
| [`memoizer`](src/main/java/org/example/memoizer/) | Sharing expensive computed values across callers by key, using `FutureTask` and `ReentrantLock`. |
| [`parallelreport`](src/main/java/org/example/parallelreport/) | Building a report from independent sections, using `ExecutorService`, `CountDownLatch`, and `ConcurrentHashMap`. |
| [`profileaggregator`](src/main/java/org/example/profileaggregator/) | Assembling a user profile from independent services, using `CompletableFuture`. |
| [`virtualthreadfetcher`](src/main/java/org/example/virtualthreadfetcher/) | Fetching many blocking resources while preserving request order, using virtual threads and `Future`. |
| [`expiringcache`](src/main/java/org/example/expiringcache/) | Serving cached values only while they are fresh, using `ReentrantLock` and `ScheduledExecutorService`. |
| [`expiringlrucache`](src/main/java/org/example/expiringlrucache/) | Keeping a bounded cache fresh and recently used, using `HashMap`, linked list, and `ReentrantLock`. |
| [`simplecountdownlatch`](src/main/java/org/example/simplecountdownlatch/) | Releasing waiters after enough completions are reported, using `ReentrantLock` and `Condition`. |
| [`fizzbuzz`](src/main/java/org/example/fizzbuzz/) | Emitting the FizzBuzz sequence from four cooperating workers, using `Semaphore`. |
| [`h2o`](src/main/java/org/example/h2o/) | Grouping hydrogen and oxygen workers into valid water molecules, using `Semaphore`. |
| [`diningphilosophers`](src/main/java/org/example/diningphilosophers/) | Sharing forks without deadlock while preserving non-neighbor concurrency, using `Semaphore` and `ReentrantLock`. |
| [`inventory`](src/main/java/org/example/inventory/) | Keeping stock counts correct when many buyers purchase limited inventory, using `ConcurrentHashMap`, `ReentrantLock`, and `AtomicInteger`. |
| [`ratelimiter`](src/main/java/org/example/ratelimiter/) | Admitting or rejecting per-user requests based on recent traffic, using `ConcurrentHashMap`, `ConcurrentLinkedQueue`, and `ReentrantLock`. |
| [`asyncjobqueue`](src/main/java/org/example/asyncjobqueue/) | Processing background jobs with retries and dead-letter capture, using `ExecutorService`, `ReentrantLock`, and `Condition`. |
| [`advancedjobqueue`](src/main/java/org/example/advancedjobqueue/) | Running scheduled, prioritized, cancellable jobs with retries, using `DelayQueue`. |
| [`stampedaccount`](src/main/java/org/example/stampedaccount/) | Keeping account balances and snapshots consistent under concurrent access, using `StampedLock`. |
| [`webcrawler`](src/main/java/org/example/webcrawler/) | Crawling each reachable same-host URL once, using `ExecutorService`, `BlockingQueue`, `ConcurrentHashMap`, and `AtomicInteger`. |

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
