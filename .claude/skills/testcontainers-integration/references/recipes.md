# Recipes & review rubric

Per-service container snippets, then the review checklist. All use the
`@Container @ServiceConnection static` pattern from `spring-boot-setup.md` unless
noted; pin image tags to a concrete version, not `latest`, so tests are
reproducible.

## Recipes

### PostgreSQL
```java
@Container @ServiceConnection
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
```
Run Liquibase as usual (the SCOS BOM pins Liquibase, not Flyway) — the changelog
applies to the container, which is the point: you verify migrations actually work
on real Postgres.

### Kafka
```java
@Container @ServiceConnection
static KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0");
```
Test producers/consumers end to end. Await assertions (Awaitility) for async
delivery rather than `Thread.sleep`.

### Redis
Redis has no dedicated module name collision — use a generic container with the
Redis `@ServiceConnection`, or the community module.
```java
@Container @ServiceConnection
static GenericContainer<?> redis =
    new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
```

### MongoDB
```java
@Container @ServiceConnection
static MongoDBContainer mongo = new MongoDBContainer("mongo:7");
```

### LocalStack (AWS)
```java
@Container
static LocalStackContainer localstack =
    new LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
        .withServices(Service.S3, Service.SQS);
```
Wire endpoints via `@DynamicPropertySource` (LocalStack needs the endpoint
override). Useful for S3/SQS/DynamoDB without touching real AWS.

## Review rubric

Order findings by impact: correctness and flakiness first, then suite speed,
then structure.

### 1. Correctness & flakiness (highest impact)
- Do tests share mutable data so they pass/fail depending on order? Each test
  should start from a known state (transactional rollback, truncate, or fresh
  schema).
- Are async effects awaited properly (Awaitility) or papered over with
  `Thread.sleep`, which is both slow and flaky?
- Are image tags pinned to a version, or floating on `latest` (non-reproducible)?
- Is the test actually hitting the container, or did `@DataJpaTest` silently fall
  back to an embedded DB (missing `@AutoConfigureTestDatabase(replace = NONE)`)?

### 2. Suite speed
- Is a container started per test class when a singleton/shared base could start
  it once? This is the most common cause of slow suites.
- Is a full `@SpringBootTest` used where a `@DataJpaTest` slice would do?
- Are heavy containers spun up for tests that don't need real infrastructure at
  all (should be unit tests)?

### 3. Wiring & structure
- Is connection wiring done with `@ServiceConnection` where available, or
  hand-rolled `@DynamicPropertySource` that could be simpler?
- Are integration tests clearly separated from unit tests (naming `*IT`, or a
  Failsafe/Surefire split) so the fast feedback loop stays fast?
- Is container reuse depended on in CI (it shouldn't be — CI runs clean)?

### Output format for a review
1. **Health summary** — one paragraph: is the suite trustworthy and is it fast
   enough to actually run often?
2. **Findings** — impact-ordered, each with why + concrete fix.
3. **What's solid** — 1–3 things worth keeping.
