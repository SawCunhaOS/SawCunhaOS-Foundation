# Epic 3 Context: Confiabilidade de Idempotência sob Concorrência e Falha do Redis

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Make the `jdempotent` module safe under real concurrency and Redis outages. Concurrent calls with the same idempotency key must never duplicate business effect: the second caller gets an explicit `409 IN_PROGRESS` (not a silent `null`), and a same-key call with a different payload gets `422 PAYLOAD_MISMATCH` instead of a shared cached response. When Redis is unavailable or slow, the module must fail open (business requests proceed) behind a circuit breaker — it must never fail closed. The guiding design principle: idempotency caching is a fast-path optimization, not the real duplicate-prevention guarantee. For any endpoint using a natural key (e.g. login, tenant+cpf), the real barrier is a `UNIQUE` constraint in the database — Redis only avoids repeated work; when it fails open, the database is what actually holds the guarantee. This epic also fixes several standalone correctness/security bugs in the module (NPE on non-Sentinel Redis, weak hashing, incomplete field-hierarchy resolution, duplicate bean registration, unsafe deserialization) that were blocking safe adoption.

## Stories

- Story 3.1: Corrigir a inicialização do `RedisConnectionFactory`
- Story 3.2: Migrar hashing de MD5 para SHA-256
- Story 3.3: Resolver campos anotados em toda a hierarquia de classes
- Story 3.4: Registrar `IdempotentAspect` sem duplicidade e sem `ThreadLocal`
- Story 3.5: Tornar a aquisição do lock atômica (`tryAcquire`/`Lease`)
- Story 3.6: Detectar colisão de payload sob a mesma chave
- Story 3.7: Garantir fail-open com circuit breaker quando o Redis está indisponível
- Story 3.8: Tornar a política de falha por exceção de negócio declarável por método
- Story 3.9: Posicionar o aspecto de idempotência fora do escopo transacional
- Story 3.10: Tornar o namespace de prefixo de chave configurável
- Story 3.11: Expor métricas de idempotência
- Story 3.12: Introduzir `IdempotencyKeyResolver` com composição de chave declarativa
- Story 3.13: Suportar header `Idempotency-Key` como fonte de chave
- Story 3.14: Migrar configuração para `@ConfigurationProperties`
- Story 3.15: Corrigir TTL e `equals`/`hashCode`
- Story 3.16: Adicionar allowlist de tipos no `PolymorphicRedisSerializer`
- Story 3.17: Substituir construtores telescópicos por builder e documentar o princípio de design

## Requirements & Constraints

- Success criterion: zero idempotency collision/duplicate processing in normal operation (Redis available). Under degradation (fail-open), duplication is an explicitly accepted risk — the guarantee shifts to the DB `UNIQUE` constraint, not the cache.
- Any endpoint with a natural idempotency key must have a `UNIQUE` DB constraint before enabling `jdempotent` — a hard prerequisite, documented in the module README and endpoint-review checklist.
- Fail-open is non-negotiable: Redis unavailability/slowness must never block a business request; a circuit breaker detects slowness against the configured Redis command timeout.
- The idempotency key must always be resolved through one single, shared key resolver — never reimplemented per entrypoint (HTTP aspect vs. messaging listener). It must return `null` cleanly (never throw) when there's no web context, since the module is not HTTP-only.
- Lease TTL is always `java.time.Duration`, never a raw numeric type in an implicit unit, and configurable per method rather than fixed.
- `X-Request-ID` must never double as an idempotency key source; header-based keys use a dedicated `Idempotency-Key` header with precedence header → annotated fields → hash.
- Test coverage: minimum 80% (jacoco) on areas touched by this epic — concurrency, hex-collision, duplicate-aspect, and Redis-unavailability scenarios (via Testcontainers). Each bug fix should be reproduced by a failing test first; no behavior change beyond what's declared per story.
- No formal migration guide or consumer notice for this release (library still SNAPSHOT, pilot consumer base) — the CHANGELOG is the only communication surface.
- Operational risks worth guarding against: (a) a client generating the idempotency key inside its own retry loop defeats deduplication — mitigated by docs plus the `.hit` metric; (b) a slow fail-open path (Redis down, every call absorbing the full timeout) can saturate the thread pool before any alert fires — mitigated by the circuit breaker and a tight Redis command timeout.

## Technical Decisions

- Lock acquisition contract: `tryAcquire(key, payloadHash, ttl) → Lease`, replacing the old non-atomic `contains → store → setResponse` flow. `Lease` states drive the HTTP outcome: in-progress → `409`, payload mismatch → `422` (mismatch takes precedence over in-progress when both conditions coincide in the same race window). Note: the architecture originally specified this as a Lua script; the actual Story 3.5 implementation uses Redis `SET NX PX` (`setIfAbsent`) instead — treat this as the accepted implementation unless a review decides otherwise.
- Circuit breaker: `io.github.resilience4j:resilience4j-spring-boot4:2.4.0` (not `-spring-boot3`), pinned explicitly in the `jdempotent` `pom.xml` — it is not managed by any project BOM. Trips via `slow-call-duration-threshold`, sized against `spring.data.redis.timeout`.
- Metrics: `IdempotencyMetrics` is a first-party interface, no-op by default, Micrometer-backed when available (never couple directly to Micrometer). Minimum set: `idempotency.acquired`, `.hit`, `.in_progress`, `.mismatch`, `.backend_error`, `.degraded` (0/1 gauge), `.degraded.transitions` (counter).
- Hashing moves from MD5/`Integer.toHexString` to SHA-256 (via a `CryptographyAlgorithm` enum) with `HexFormat.of().formatHex()`; annotated-field resolution must walk the full class hierarchy, not just declared fields.
- `IdempotentAspect` must self-register with `@ConditionalOnMissingBean` to avoid duplicate beans across multiple registration points, and must not use `ThreadLocal` for `MessageDigest` (breaks under virtual threads).
- The aspect's `@Order` must sit outside the `@Transactional` scope so a transaction rollback never leaves an orphaned key in Redis.
- Redis connection must come from the Spring Boot-managed `RedisConnectionFactory` instead of a hand-assembled `RedisSentinelConfiguration` — the latter NPEs on `getSentinel()` for standalone/cluster Redis.
- Key namespace prefix becomes a mandatory, explicit Spring `@ConfigurationProperties` value (fails fast if unset) — replacing the current silent `System.getenv(APP_NAME)` fallback. Configuration overall migrates from loose `@Value` to `@ConfigurationProperties`, and the module's `EnvironmentPostProcessor` (currently misdeclared in `AutoConfiguration.imports` and never executing) must be fixed to actually run.
- Business-exception failure policy is declarable per method: `RELEASE` (default, removes the key, allows retry) or `KEEP_FAILED` (records the error as the result, prevents a retry from duplicating a side effect).
- `PolymorphicRedisSerializer` needs a type allowlist checked before `Class.forName` on values coming back from Redis (arbitrary-type deserialization risk).
- Telescoping constructors are replaced by a builder; the module README must state the "cache is fast-path, not the guarantee" principle, the `UNIQUE`-constraint prerequisite, and the recommended ordering of the module's three independent timeouts: lease `ttl` > circuit breaker `slow-call-duration-threshold` > `spring.data.redis.timeout` (with margin) — exact sizing is left to each consuming team, but this relative order must hold so ownership of the lock is unambiguous when a timeout fires.
- Test convention for this module: unit tests use the default `*Test` suffix (Surefire, run on `mvn test`); anything standing up Testcontainers/Docker uses the `*ITTest` suffix, bound to Failsafe's `integration-test`/`verify` phases (`mvn verify`). Concurrency/topology tests must exercise the `tryAcquire(...) → Lease` contract only — never the old `contains`/`store`/`setResponse` path.

## Cross-Story Dependencies

- Recommended internal build order: key-correctness fixes (3.2, 3.3, 3.4) → concurrent-execution contract (3.5, 3.1, 3.8, 3.9, 3.10) → declarative composite key (3.12) → header key source (3.13) → hardening, which can proceed in parallel (3.14–3.17).
- Story 3.6 (payload mismatch, `422`) takes precedence over Story 3.5's `409 IN_PROGRESS` whenever both conditions coincide in the same race window.
- Story 3.13's header key source, and any future messaging entrypoint, both depend on the single `IdempotencyKeyResolver` introduced in Story 3.12.
- Story 3.11's metrics depend on instrumentation added by other stories: `in_progress`/`hit`/`acquired` from 3.5, `mismatch` from 3.6, `backend_error`/`degraded`/`degraded.transitions` from 3.7.
- If schedule pressure forces a scope cut, PM guidance is explicit: 3.12, 3.13, and 3.14–3.17 (hardening) can be deferred; the concurrency/fail-open/security core (roughly 3.1, 3.2, 3.3, 3.5, 3.6, 3.7, 3.9) already delivers a correct module and should not be cut — never ship a non-atomic lock or a silent fail-closed behavior.
- Soft dependency on Epic 1's module split: the `@Jdempotent*` annotations must already live in `jdempotent-api` before this epic's changes land, to avoid touching the same classes twice.
