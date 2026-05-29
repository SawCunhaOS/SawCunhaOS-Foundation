# scos-foundation — module & feature catalog

The `scos-foundation` project (package root `br.com.sawcunhaos.foundation`) ships
four modules. Depend on them via the SCOS BOM. Imports below are the real
foundation paths.

## Dependency coordinates

| Module      | artifactId                     | Provides                                    |
| ----------- | ------------------------------ | ------------------------------------------- |
| utils       | `scos-foundation-utils`        | annotations, DTOs, validators, cache, LGPD  |
| exception   | `scos-foundation-exception`    | `ExceptionsHandler`, `ScosException` family |
| audit       | `scos-foundation-audit`        | `@Auditable` + Hibernate audit log          |
| jdempotent  | `scos-foundation-jdempotent`   | idempotency (in-memory / Redis)             |

groupId `br.com.sawcunhaos`. **Version note:** the foundation's own pom imports
`br.com.sawcunhaos:scos-bom:1.0.0`, while the public BOM README shows
`io.github.sawcunha:scos-bom`. Confirm the canonical groupId with the team before
relying on either. Foundation module versions are managed centrally — prefer
declaring them without an explicit `<version>` and let the BOM resolve them; pin
only if the BOM does not manage them.

## Web — `br.com.sawcunhaos.foundation.utils.annotation`

- `@ScosController` — `@RestController` + `@RequestMapping("/api", produces=JSON)`.
- `@ScosRequestGET(uri, httpCode, nameCache, keyGenerator)` — GET +
  `@ResponseStatus` + `@Cacheable` (GETs are cacheable by default).
- `@ScosRequestPOST` / `@ScosRequestPUT` / `@ScosRequestDELETE(uri, httpCode, consumes)`
  — write methods + status.
- `@ScosRequestMapping(method, uri, httpCode, consumes)` — the base meta-annotation.

## DTOs — `...utils.dto`

- `ScosResponseDTO<T>` (`response`) — standard envelope: `data` + optional
  `scosPaginatedDTO`. Built with Lombok `@Builder`.
- `ScosPaginatedDTO` (`response`) — record: `sizePerPage`, `totalPages`,
  `totalElements`, `totalElementsPerPage`.
- `ScosPaginationFilterDTO` (`request`) — inbound paging/filter/sort input.
- Helpers: `ScosResponseUtils`, `PaginationUtils`.

## Exceptions — `br.com.sawcunhaos.foundation.exception`

- `ExceptionsHandler` — global `@ControllerAdvice extends ResponseEntityExceptionHandler`;
  localizes via `LocaleService`. Don't duplicate it.
- `error.ScosException(ExceptionCode, Object... args)` — base; carries `code` +
  `args`, message resolved by i18n.
- `error.ScosNoContentException`, `error.ScosNoRollbackException`,
  `error.ScosSecurityException`, `error.MethodNotImplementedException`.
- `model.ExceptionResponse(message, codeError, validationErrors)` — the JSON shape
  returned to clients (`@JsonInclude(NON_NULL)`).
- `model.AttributeNotValid` — per-field validation error entry.
- `ExceptionCode` contract lives in `...utils.specification`; implement a project
  enum providing `getCode()` and map codes to i18n message keys.

## Auditing — `scos-foundation-audit`

- `@Auditable` (`...utils.annotation.audit`) — mark entities/operations to audit.
- Driven by a Hibernate post-event listener (`ScosHibernateAuditListener`) →
  `ScosAuditService` (`specification`) / `ScosAuditServiceBean` (`@Async`,
  `@ConditionalOnProperty(prefix="scos.audit", name="enabled", havingValue="true")`).
- Persists `ScosAuditLog` (with `ActionType`) to a configurable datasource;
  Liquibase-managed. Enable with `scos.audit.enabled=true`.

## Idempotency — `scos-foundation-jdempotent`

- Annotations (`...utils.annotation.jdempotent`): `@JdempotentResource` (mark an
  endpoint/method), `@JdempotentRequestPayload`, `@JdempotentId`,
  `@JdempotentProperty`, `@JdempotentIgnore`.
- Aspect-based (`IdempotentAspect`); pluggable store: `InMemoryIdempotentRepository`
  or `RedisIdempotentRepository`. Use it instead of hand-rolling dedup logic for
  retried POSTs / message handlers.

## Caching, LGPD, validation, misc — `scos-foundation-utils`

- **Cache:** `ScosCacheConfiguration`, `ScosCacheKeyGenerator`,
  `PolymorphicRedisSerializer`, `ScosCacheProperties`. `@ScosRequestGET` ties into
  this via its `nameCache`/`keyGenerator` attributes.
- **LGPD / PII:** `@DataMask` + `DataMaskingService`; request/response sanitization
  components for headers and body.
- **Brazilian validators** (`...utils.validation`): `@CPF`, `@CNPJ`,
  `@TaxIdentifier`, `@ZipCode` (with Jakarta `ConstraintValidator`s). Use these on
  DTOs/value objects instead of regex.
- **String normalization:** `@NormalizeStrings` + `StringProcessingAspect`.
- **Security/context:** `ScosUserAuthentication` (current user), `ScosPermission`,
  `ScosFeature`.
- **i18n:** `LocaleService`.
- **Persistence helpers:** `SpecificationRepository` and `SpecificationFunction`
  for dynamic JPA Specifications; `PropertiesOrder` for sorting.
- **Serialization adapters:** `LocalDate/LocalDateTime/LocalTimeAdapter`,
  `GsonUtils`, `JacksonXmlUtils`, `JacksonCustomJsonFormatMapper` (Hibernate JSON).
- **Feign:** `JacksonDecoderCustom` / `JacksonEncoderCustom`.
- **Lifecycle:** `ScosOnStartupListener` / `ScosStartupListener`.

## Config keys seen in the foundation

- `spring.main.allow-bean-definition-overriding: true`
- `spring.jpa.properties.hibernate.type.json_format_mapper:
  br.com.sawcunhaos.foundation.utils.configuration.hibernate.JacksonCustomJsonFormatMapper`
- `scos.audit.enabled: true` (+ audit datasource/liquibase properties)
- Redis properties for jdempotent/cache when those modules are used.
