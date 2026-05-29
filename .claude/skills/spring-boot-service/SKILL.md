---
name: spring-boot-service
description: >
  Scaffold and review Spring Boot 4 services — the project skeleton, REST API
  layer, configuration, and error handling that wrap a domain model. Use this
  skill whenever the user is starting a new Spring Boot service or module, asks
  for a pom.xml / project setup, wants REST controllers with DTOs and validation,
  needs centralized error handling (RFC 7807 / ProblemDetail), is setting up
  application.yml/profiles/externalized config, or wants a review of a service's
  web layer and structure — even if they don't name "Spring Boot" explicitly.
  Targets the current stable Spring Boot 4.0.x / Spring Framework 7 / Java 25,
  Maven, and the user's SCOS BOM for dependency management. For the domain model
  itself use ddd-tactical-design; for tests use tdd-workflow and
  testcontainers-integration. For security and observability use
  spring-security-scos and observability-otel.
---

# Spring Boot Service (4.0.x / Java 25 / Maven)

This skill builds and reviews the "outer shell" of a service: the Maven project,
the REST/web layer, configuration, and error handling. It deliberately leaves the
domain model to `ddd-tactical-design` — the web layer should be thin and delegate
inward to application services. Targets Spring Boot 4.0.x on Spring Framework 7
(JSpecify null-safety, Jakarta EE, native OpenTelemetry observability, stable
HTTP API versioning), Java 25, and Maven.

## Project foundation — the SCOS BOM (required)

Every project uses the user's Bill of Materials, `br.com.sawcunhaos:scos-bom`,
which pins the entire stack (Spring Boot 4.0.x, Framework 7, Spring
Data/Cloud/Kafka, Hibernate 7, Jackson 3, Liquibase, JUnit Jupiter, Mockito,
Testcontainers, REST Assured, WireMock, Lombok, MapStruct, and more). Import it
in `dependencyManagement` and declare dependencies WITHOUT versions; the BOM is
the single source of truth, which is how the stack stays aligned and current. Do
not also use `spring-boot-starter-parent` as the parent or pin versions the BOM
controls. See `references/project-skeleton.md` for the full pom.

## SCOS house style (apply the scos-conventions skill)

These services build on `scos-foundation`, so the web layer uses SCOS idioms, not
raw Spring. Apply the `scos-conventions` skill alongside this one. Concretely:
controllers use `@ScosController` + `@ScosRequestGET/POST/PUT/DELETE` (not
`@RestController`/`@GetMapping`); endpoints return `ScosResponseDTO<T>`; and
errors are signaled by throwing `ScosException(ExceptionCode, args...)` — the
foundation's `ExceptionsHandler` is global, so do NOT add a per-project
`@RestControllerAdvice`/`ProblemDetail` handler. Also add the foundation
dependencies (`scos-foundation-utils`, `scos-foundation-exception`, and audit/
jdempotent as needed).

## First step: decide the mode

- New service/module, pom, REST endpoint, config → **Generate**.
- Reviewing an existing service's structure or web layer → **Review**.

Read `references/project-skeleton.md` (pom + layout + config) and
`references/rest-and-errors.md` (controllers, DTOs, validation, ProblemDetail,
API versioning) before producing substantial output.

## Generate

1. Scaffold the pom from `references/project-skeleton.md` — SCOS BOM imported,
   starters version-free, `spring-boot-maven-plugin` for packaging.
2. Lay out packages per the `ddd-tactical-design` layering: `api`, `application`,
   `domain`, `infrastructure`. This skill owns `api` and the wiring; the domain
   comes from the DDD skill.
3. Build the web layer thin, in SCOS style (see the `scos-conventions` skill):
   - `@ScosController` classes with `@ScosRequest*` methods; accept/return DTOs
     (records), never domain aggregates or JPA entities.
   - Endpoints return `ScosResponseDTO<T>`; paged endpoints use `ScosPaginatedDTO`
     and accept `ScosPaginationFilterDTO`.
   - Bean Validation (`@Valid`, `jakarta.validation` + the foundation's BR
     validators like `@CPF`/`@CNPJ`/`@ZipCode`) on request DTOs; map DTO ↔
     command/domain with MapStruct.
   - One application-service call per endpoint; no business logic in controllers.
4. Don't write error-handling code — throw `ScosException(ExceptionCode, args...)`
   from the application layer and let the foundation's global `ExceptionsHandler`
   produce the localized `ExceptionResponse`. Define a project `ExceptionCode`
   enum with i18n message keys.
5. Configuration in `application.yml` with profiles; externalize secrets via
   environment variables, never commit them. Enable virtual threads
   (`spring.threads.virtual.enabled: true`) — on Java 25 + Boot 4 this is a cheap
   throughput win for the typical blocking-IO service. Expose Actuator health/info
   for orchestration probes.
6. If versioning the API, use Spring 7's built-in HTTP API versioning rather than
   hand-rolled path/header schemes.

## Review

Assess against `references/review-checklist.md`. The findings that recur most:
controllers doing business logic or returning entities/aggregates straight to the
client, ad-hoc error responses instead of a single ProblemDetail advice, and
secrets or environment-specific values baked into committed config. Lead with
whichever is worst.

Frame the review as a one-paragraph health summary, then impact-ordered findings
(layering/leaks > error handling > config/secrets > validation > naming), each
with the why and a minimal fix.

## How this fits the suite

The web layer translates HTTP into application calls and back; it should be
boring. Richness lives in the domain (`ddd-tactical-design`), correctness is
proven by tests (`tdd-workflow`, `testcontainers-integration`, `api-testing-k6`),
and the service is secured and made observable via `spring-security-scos` and
`observability-otel`. Keep concerns where they belong rather than
letting the controller become the place everything happens.
