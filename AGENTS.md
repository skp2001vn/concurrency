# AGENTS.md

## Purpose

This repository contains small, focused Java concurrency examples used for practice, learning, and interview preparation.

Each example should remain:
- self-contained
- easy to read
- correct under concurrent access
- backed by automated tests

## Tech Stack

- Java 17
- Maven
- JUnit 5

## Project Structure

- `src/main/java/org/example/<example>/`
  - production code for each concurrency example
- `src/test/java/org/example/<example>/`
  - tests for the matching example
- `README.md`
  - high-level project overview and example list

## Implementation Guidelines

- Keep each example package focused on one problem or concurrency pattern.
- Prefer clear, standard Java concurrency primitives such as:
  - `ReentrantLock`
  - `Condition`
  - `Semaphore`
  - `Atomic*`
  - `ConcurrentHashMap`
  - `BlockingQueue`
  - `ExecutorService`
- Favor correctness and readability over cleverness.
- Avoid unnecessary abstractions for small examples.
- Keep APIs minimal and aligned with the problem being solved.
- Use descriptive class and method names. Prefer simple names like `WebCrawler` over overly specific or verbose names unless needed.

## Javadoc Expectations

- Add class-level Javadoc for each example class.
- Add method-level Javadoc for public methods.
- Javadoc should explain:
  - the concurrency problem being solved
  - the coordination strategy used
  - important behavioral guarantees or constraints
- Keep Javadoc concise and practical. Avoid repeating obvious implementation details line by line.

## Testing Guidelines

- Every new example should include dedicated JUnit 5 tests.
- Tests should validate:
  - normal behavior
  - edge cases
  - concurrency coordination behavior
  - regressions around ordering, blocking, deduplication, or termination when relevant
- Prefer deterministic tests over timing-sensitive tests.
- Use helper fakes/stubs where appropriate instead of external dependencies.
- After changes, run:

```bash
mvn test
```

## Code Style

- Follow the existing project style and keep formatting consistent.
- Prefer simple package layouts: one package per example.
- Keep comments minimal and high signal.
- Use ASCII unless the file already requires something else.
- Do not add unused code, unused imports, or speculative utilities.

## README Maintenance

- Update `README.md` when adding a new example.
- Keep the example list and test-count statement in sync with the current repository state.

## When Adding A New Example

1. Add production code under `src/main/java/org/example/<example>/`
2. Add tests under `src/test/java/org/example/<example>/`
3. Add Javadoc to public classes and methods
4. Update `README.md`
5. Run `mvn test`

## Non-Goals

- Do not turn this repository into a framework or reusable library unless explicitly requested.
- Do not introduce unnecessary dependencies for simple examples.
- Do not sacrifice clarity just to mimic highly optimized production code.
