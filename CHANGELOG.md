# Changelog

All notable changes to SCOS Foundation are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/) and this project adheres to semantic versioning.

## [Unreleased]

### Added — `scos-foundation-audit` module (accountability / LGPD)

- **Batch pipeline**: `ScosAuditBatchConsumer` drains `ConcurrentLinkedQueue` by size (default 100) or
  interval (default 500 ms) via a dedicated virtual thread; `saveAll` replaces `saveAndFlush` — N round-trips
  → 1 per batch. Configurable via `scos.audit.performance.*`.
- **Durability**: retry with exponential backoff (`scos.audit.durability.retry-max`, default 3); DLQ table
  `SFA_AUDIT_DLQ` stores failed batches; `@Scheduled` job reprocesses up to 50 entries per run.
- **Hash-chain / tamper-evidence**: opt-in via `scos.audit.immutability.hash-chain=true`; SHA-256 chain
  per `(entity, idEntity)`; `ScosAuditIntegrityService.verifyChain()` detects tampering and missing records.
- **Audit query**: `ScosAuditQueryService` exposes paginated queries by entity/idEntity, user, period, and
  `xRequestId`; backed by new JPA queries + indices on `SFA_LOG_AUDIT`.
- **READ coverage**: `@Auditable` now accepts `action = AuditAction.READ` (method-level); `ScosAuditReadAspect`
  emits `ActionType.SELECT` after successful return; `ScosAuditService.recordRead()` for manual bulk/JPQL ops.
- **Retention**: `ScosAuditRetentionJob` purges expired records (`scos.audit.retention.ttl-days`); inserts
  tombstone record (`ActionType.TOMBSTONE`) to preserve hash-chain integrity. Opt-in via `retention.enabled`.
- **Observability**: `audit.queue.depth` (Gauge), `audit.batch.size` (DistributionSummary),
  `audit.events.dlq` (Counter) via Micrometer `ObjectProvider` — no-op when Micrometer is absent.
- **Integration tests**: Testcontainers + PostgreSQL covering burst batching (≥500 events), retry, DLQ,
  backpressure, hash-chain (tamper detection), paginated query, READ coverage, retention/tombstone, metrics.

### Changed — `scos-foundation-audit`

- `@Auditable` annotation (in `scos-foundation-utils`) extended with `action()`, `entity()`,
  `idEntitySpEL()` attributes — backward-compatible (all defaults preserved, existing class-level usage unchanged).
- `ActionType` enum gains `TOMBSTONE` value.

### **BREAKING** — `scos-foundation-audit`

- **Liquibase migration required**: `SFA_LOG_AUDIT` gains nullable column `HASH_CHAIN VARCHAR(64)`;
  new table `SFA_AUDIT_DLQ` created; new composite index `IDX_ILA_ENTITY_ID` on `(ENTITY, ID_ENTITY)`.
  Run migration before deploying; old records have `HASH_CHAIN = NULL` (chain starts from first new record).
- **Grants**: to enforce append-only policy, revoke `UPDATE` and `DELETE` on `SFA_LOG_AUDIT` for the
  application role: `REVOKE UPDATE, DELETE ON SFA_LOG_AUDIT FROM <your_app_role>;`

### **BREAKING** — Gson removed, Jackson-only JSON (`scos-foundation-utils` / `audit` / `privacy`)

- **Wire format change**: `java.time` values (`LocalDateTime`/`OffsetDateTime`/etc.) are now serialized by
  Jackson's native `java.time` support (`jackson-datatype-jsr310`), not by the 3 custom Gson
  `TypeAdapter`s removed in this release (`LocalDateAdapter`, `LocalDateTimeAdapter`, `LocalTimeAdapter`).
  Any consumer parsing these date/time fields as raw text should verify compatibility with Jackson's
  default `java.time` representation before upgrading.
- `GsonUtils` (in `scos-foundation-utils`) is removed — no replacement; callers use Jackson's
  `ObjectMapper` (Spring-managed where available) directly.
- `gson` dropped from the dependency tree of `scos-foundation-utils` and `scos-foundation-privacy`.
- `privacy/core/JsonMasker` now depends on Jackson (`tools.jackson.core:jackson-databind`), added directly
  to `scos-foundation-privacy`'s own POM — the module still does not depend on `utils`.
- `scos-foundation-audit`'s hash-chain input (`ScosAuditHashService.canonicalizeJson`), the DLQ payload
  (`ScosAuditBatchConsumer`/`ScosAuditDlqJob`), and the entity-state snapshot
  (`ScosAuditServiceBean.createJsonObject`) all move from Gson to Jackson. Any hash-chain already
  persisted before this release does not verify against the new serializer (the JSON bytes the hash is
  computed over changed) — reprocess or discard existing chains before upgrading, do not migrate on top
  of them.
- Null policy preserved explicitly: `ScosAuditLog` is annotated `@JsonInclude(JsonInclude.Include.ALWAYS)`
  so the DLQ payload keeps including null fields, matching the old `GsonUtils` (`serializeNulls()`)
  instance. See `audit/README.md`.

### Added — `scos-foundation-jdempotent` module

- **Fail-open circuit breaker around Redis calls**: `RedisIdempotentRepository` wraps every Redis
  operation (`contains`/`getResponse`/`store`/`remove`/`setResponse`/`tryAcquire`) in a single
  programmatic `CircuitBreaker` (`io.github.resilience4j:resilience4j-spring-boot4:2.4.0`,
  `optional=true`); once Redis is confirmed slow/down, later calls short-circuit immediately
  instead of each paying the full Redis command timeout again. `slow-call-duration-threshold` is
  resolved from the Lettuce connection factory's configured command timeout at construction time.
  Fail-open behavior is unchanged for callers — a slow/unavailable Redis never blocks or fails the
  business request (never `FAIL_CLOSED`); the accepted, documented risk is that a response may not
  get cached in that window (a retry re-executes), with the database `UNIQUE` constraint as the
  real duplicate-prevention guarantee.
- **Declarable failure policy per method**: `@JdempotentResource` gains
  `onBusinessException()` (`IdempotentFailurePolicy`, new enum in `jdempotent-api`), defaulting to
  `RELEASE` — identical to the previous behavior, the idempotency key is removed when the method
  throws, so a retry re-executes it. Methods that opt into `KEEP_FAILED` instead keep the key and
  record the failure, so a retry with the same key throws `IdempotentReplayedFailureException`
  instead of re-executing the method — useful when the method already produced a side effect before
  failing. Only the original exception's class name and message survive the replay, not the original
  exception instance or type: the original business exception is not cacheable as-is (most don't
  survive a real Redis round trip), so it is recorded as an encoded `String` instead.
- **Configurable key-prefix namespace**: new `ScosJdempotentProperties`
  (`@ConfigurationProperties(prefix = "scos.jdempotent")`) exposes `scos.jdempotent.namespace`,
  injected into `ScosJdempotentConfig` to build `DefaultKeyGenerator(namespace)` for the
  auto-configured `IdempotentAspect` beans. See BREAKING note below for the behavior change.
- **Idempotency observability**: `idempotency.acquired`/`.hit`/`.in_progress`/`.mismatch`/
  `.backend_error` (counters), `idempotency.degraded` (gauge, 0/1) and
  `idempotency.degraded.transitions` (counter) via the new `IdempotencyMetrics` abstraction —
  no-op by default, Micrometer-backed (`MicrometerIdempotencyMetrics`) only when Micrometer is on
  the consumer's classpath (`@ConditionalOnClass(MeterRegistry.class)`), registered by the new
  `ScosJdempotentMetricsConfiguration` auto-configuration. `idempotency.in_progress` is the
  production-detection signal for a lease expiring before the protected method finishes (Story 3.5,
  AC #4); `idempotency.degraded` reflects the `RedisIdempotentRepository` circuit breaker (Story
  3.7) being anywhere but `CLOSED`, flipping (and incrementing `.degraded.transitions`) only on an
  actual normal/degraded transition, not on every check.

### **BREAKING** — `scos-foundation-jdempotent`

- **`scos.jdempotent.namespace` is now required for the Spring auto-configuration path**
  (`ScosJdempotentConfig`): previously, an application without the `APP_NAME` environment variable
  started normally and silently generated idempotency keys with no namespace prefix — a real risk
  of key collision when two different applications share the same Redis instance. Now the Spring
  context **fails to start** (both a `@NotBlank`/`@Validated` check and an unconditional
  `@PostConstruct` check on `ScosJdempotentProperties`, so the failure does not depend on a Bean
  Validation provider being present on the consumer's classpath) if the property is not set.
  **Action required before upgrading**: set `scos.jdempotent.namespace` (e.g. the application name)
  in every consumer's configuration; there is no automatic fallback to the old `APP_NAME`
  environment variable. This only affects apps going through `ScosJdempotentConfig` — code that
  builds `IdempotentAspect`/`DefaultKeyGenerator` programmatically, outside Spring
  auto-configuration, keeps working with no namespace prefix (`DefaultKeyGenerator`'s no-arg
  constructor no longer reads `System.getenv` either, it just never adds a prefix).
- `EnvironmentVariableUtils` (and its `APP_NAME` constant) removed — no longer used.

### Added — `scos-foundation-privacy` module

- New base-layer module **`scos-foundation-privacy`** providing a high-performance, multithread-safe
  `MaskingEngine` (immutable, lock-free, pre-compiled patterns, O(1) key lookup, single-pass Aho-Corasick,
  in-place JSON walk, zero-allocation fast-path, ReDoS rejection at build time).
- **YAML-driven rules** (`privacy-masking.yml`): masking fields and strategies are configuration data — no
  database, no recompile. Source resolution: external `scos.privacy.masking.config-path`
  (or env `SCOS_PRIVACY_MASKING_CONFIG`) → classpath fallback → builtins + WARN.
- **Portable core**: `MaskingEngine.fromYaml(Path|InputStream)` runs outside the SCOS layer, without Spring.
- **Masking strategies** as a closed enum: `fixed`, `partial`, `email`, `hash` (keyed HMAC), `encrypt`
  (reversible), `redact`; validated at startup with a `partial` fail-safe.
- **Builtins by country/region** as embedded data packs: `generic`, `br`, `us`, `eu`, `uk`, `in`; enable by
  list, disable individual items; check-digit validation for `cpf`/`cnpj`/`credit-card`.
- **Auto-configuration** via `AutoConfiguration.imports` — loads on import, every bean
  `@ConditionalOnMissingBean`; optional `DataMaskingValues` SPI added on top of the YAML
  (precedence builtins → YAML → SPI).
- **Application-log masking**: Logback `%mask` converter registered programmatically (no `logback.xml`
  edit); opt out via `scos.privacy.log.register-converter=false`.
- **Audit at-rest encryption**: `ScosFieldCipher` (AES-256/GCM) with `enc:vN:` tokens that embed the key id;
  `ScosCryptoKeyProvider` SPI (default externalized secret; plug Vault/KMS). Key is versioned per record;
  history is not re-encrypted.

### Changed

- **BREAKING** (planned): masking moves from package `br.com.sawcunhaos.foundation.utils.lgpd` to
  `br.com.sawcunhaos.foundation.privacy`. Deprecated facades in the old package will delegate for one
  release. `utils` and `audit` gain a dependency on `scos-foundation-privacy`; the parent POM lists
  `privacy` before `utils`.
- Recommended way to declare masking rules changes from implementing `DataMaskingValues` in code to the
  `privacy-masking.yml` file; the SPI remains as an optional override.

### Notes

- Audit JSONB columns may contain encrypted values (`enc:vN:`) for fields opted in via
  `audit-encrypt-fields`; reading them directly in the database requires decryption. The feature is opt-in
  per field — an empty list keeps current behaviour.
