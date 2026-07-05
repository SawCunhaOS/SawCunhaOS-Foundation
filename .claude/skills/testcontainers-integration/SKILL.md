---
name: testcontainers-integration
description: >
  Write and review integration tests for Java/Spring Boot using Testcontainers —
  real databases, message brokers, and other backing services in disposable
  Docker containers. Use this skill whenever the user wants integration tests
  against a real Postgres/MySQL/MongoDB/Kafka/Redis/LocalStack, mentions
  Testcontainers, @ServiceConnection, @DynamicPropertySource, @DataJpaTest with a
  real database, flaky or slow integration tests, or testing that JPA mappings /
  SQL migrations / queries actually work against the real engine — even if they
  don't name Testcontainers. Also trigger for "test against a real database
  instead of H2" and for speeding up an integration suite. Targets Testcontainers
  with the current stable Spring Boot 4.0.x / Java 25 / JUnit 5.
---

# Testcontainers Integration Testing (Spring Boot 4.0.x / Java 25)

This skill covers integration tests that exercise real backing services in
throwaway Docker containers, so you test against the engine you actually run in
production instead of an H2 stand-in that quietly behaves differently. Targets
Spring Boot 4.0.x, JUnit Jupiter, and Spring Boot's first-class Testcontainers
support (`@ServiceConnection`), which auto-wires connection details with no
manual property plumbing.

## Project foundation — the SCOS BOM (required)

The user's BOM (`br.com.sawcunhaos:scos-bom`) already manages Testcontainers,
JUnit Jupiter, and the JDBC drivers. Import the BOM and declare the
Testcontainers modules WITHOUT versions; crucially, do NOT import the
`testcontainers-bom` as well — that would double-manage and can conflict. The BOM
also pins **Liquibase** (not Flyway) for migrations, so integration tests verify
Liquibase changelogs against the real engine.

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>br.com.sawcunhaos</groupId>
      <artifactId>scos-bom</artifactId>
      <version>1.0.0</version> <!-- track the latest SCOS BOM release -->
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

## First step: decide the mode

- Adding integration tests, or wiring Testcontainers into a project → **Generate**.
- Judging or speeding up an existing integration suite → **Review**.

Read `references/spring-boot-setup.md` for the canonical wiring (it's the part
people get wrong most), and `references/recipes.md` for per-service snippets
(Postgres, Kafka, Redis, MongoDB, LocalStack).

## When an integration test is the right tool

Integration tests are slower and heavier than unit tests, so spend them where
they earn their keep: repository adapters hitting real SQL, schema migrations
(Liquibase, per the SCOS BOM) actually applying, queries that use engine-specific features,
message consumers/producers against a real broker. Pure domain logic and
application-service orchestration belong in fast unit tests (see the
`tdd-workflow` skill) — don't boot a container to test a value object.

## Generate

1. Add dependencies via the user's SCOS BOM — it already manages Testcontainers
   (2.x) and JUnit Jupiter, so do NOT import the `testcontainers-bom` separately
   and do NOT pin versions. Just declare the modules you need
   (`testcontainers`, the service module like `postgresql`, `junit-jupiter`, and
   Spring Boot's `spring-boot-testcontainers`) in `test` scope, version-free. See
   `references/spring-boot-setup.md` for the exact Maven block.
2. Choose the wiring style:
   - **`@ServiceConnection`** — preferred. Spring auto-configures datasource /
     broker connection from the container; no `@DynamicPropertySource` needed.
   - **`@DynamicPropertySource`** — fallback for services without a
     `@ServiceConnection` integration, or when you need custom property mapping.
3. Manage container lifecycle for speed:
   - For a single test class, `@Testcontainers` + `@Container` is fine.
   - Across many classes, use the **singleton container pattern** (one container
     started once for the whole suite) or a shared base class — starting a fresh
     Postgres per class is the usual reason suites crawl.
   - Enable **container reuse** locally for an even tighter loop.
4. Use slices where possible: `@DataJpaTest` (with the real DB instead of the
   default embedded one) for repository mappings; full `@SpringBootTest` only
   when you need the whole wiring.
5. Keep tests independent: clean or isolate data between tests (transactional
   rollback, truncate, or a fresh schema) so order never matters.

Default build tool is Maven. Use JUnit 5 throughout (JUnit 4 is removed in
Spring Boot 4).

## Review

Assess against `references/review-checklist.md`. The two findings that come up
most: (a) starting containers far more often than necessary, wrecking suite
time, and (b) tests that share state and pass or fail depending on order. Lead
with whichever bites hardest.

Frame the review as a short health summary, then impact-ordered findings
(correctness/flakiness > suite speed > structure), each with a concrete fix and,
where useful, a before/after of the container setup.
