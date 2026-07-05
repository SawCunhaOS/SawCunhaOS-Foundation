---
name: scos-conventions
description: >
  Follow the SawCunha Open System (SCOS) house style when writing or reviewing
  any Java/Spring code for SCOS projects, which build on the scos-foundation
  libraries. Use this skill whenever generating controllers, services, DTOs, or
  exception handling for a SCOS/SawCunha project, or reviewing such code for
  conformance — even if the user doesn't name the convention. Triggers on the
  br.com.sawcunhaos package space, the Scos* class prefix, @ScosController /
  @ScosRequestGET/POST/PUT/DELETE, ScosResponseDTO, ScosException, the
  specification-interface + Bean-implementation pattern, and the foundation
  modules (utils, exception, audit, jdempotent). Apply ALONGSIDE the domain/test
  skills (ddd-tactical-design, spring-boot-service, tdd-workflow,
  testcontainers-integration): those define the architecture, this overrides the
  concrete Spring idioms with the SCOS house style. Targets Spring Boot 4.0.x /
  Java 25 / Maven.
---

# SCOS Conventions (SawCunha Open System house style)

SCOS projects don't use raw Spring idioms directly — they build on the
`scos-foundation` libraries, which provide custom annotations, response/exception
models, and cross-cutting features. When this skill is active, prefer the SCOS
way over the vanilla Spring way. The other suite skills define the architecture
(layering, DDD, tests); this one pins the concrete idioms. Where they conflict,
this skill wins for SCOS code.

Read `references/foundation-modules.md` for the catalog of annotations,
utilities, and dependency coordinates before writing or reviewing code.

## Packages & naming

- Base package: `br.com.sawcunhaos.<project>.<module>...`.
- `Scos`-prefixed names are reserved for shared/framework-level types
  (configurations, foundation extensions). Application types use plain domain
  names.

## Dependencies — BOM + foundation modules

Import the SCOS BOM and the foundation modules you need; declare them WITHOUT
versions where the BOM manages them. (See the version note in
`references/foundation-modules.md` — confirm the BOM groupId with the team.)

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

<dependencies>
  <dependency>
    <groupId>br.com.sawcunhaos</groupId>
    <artifactId>scos-foundation-utils</artifactId>
  </dependency>
  <dependency>
    <groupId>br.com.sawcunhaos</groupId>
    <artifactId>scos-foundation-exception</artifactId>
  </dependency>
  <!-- add scos-foundation-audit / scos-foundation-jdempotent as needed -->
</dependencies>
```

## The service pattern (interface in `specification`, impl as `...Bean`)

This is the defining class-structure convention. Define the contract as an
interface in a `specification` package; implement it in a `service` package with
a `Bean` suffix, registered as a named bean, with constructor injection via
Lombok and a logger. Gate optional features with `@ConditionalOnProperty`.

```java
// ...domain.specification (or module.specification)
public interface OrderService {
    OrderId place(PlaceOrderCommand command);
}

// ...service
@Service("OrderService")
@RequiredArgsConstructor
@Slf4j
public class OrderServiceBean implements OrderService {

    private final OrderRepository orders;          // constructor-injected, final

    @Override
    public OrderId place(PlaceOrderCommand command) {
        log.debug("placing order for customer {}", command.customerId());
        // ...
    }
}
```

Field injection is not the house style — always constructor injection via
`@RequiredArgsConstructor` over `final` fields.

## Web layer — @ScosController and @ScosRequest*

Don't use `@RestController` + `@GetMapping` directly. `@ScosController` already
bundles `@RestController` + base path `/api` + JSON. The `@ScosRequest*`
annotations bundle the HTTP method and response status (and GET is `@Cacheable`).

```java
@ScosController
public class OrderController {

    private final OrderService orderService;       // the specification interface

    @ScosRequestPOST(uri = "/orders", httpCode = HttpStatus.CREATED)
    public ScosResponseDTO<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
        var id = orderService.place(request.toCommand());
        return ScosResponseDTO.<OrderResponse>builder().data(new OrderResponse(id.value())).build();
    }
}
```

Return `ScosResponseDTO<T>` from endpoints (its `data` holds the payload). For
paged results, populate `ScosPaginatedDTO` and accept `ScosPaginationFilterDTO`
for filter/sort/page input; use `ScosResponseUtils` / `PaginationUtils` helpers.

## Errors — throw ScosException, never a per-project advice

The foundation's `ExceptionsHandler` (`@ControllerAdvice`) is already global and
turns exceptions into a localized `ExceptionResponse` (`message`, `codeError`,
`validationErrors`). Do NOT write your own `@RestControllerAdvice`. Signal errors
by throwing:

- `ScosException(ExceptionCode, args...)` — general business error.
- `ScosNoContentException`, `ScosNoRollbackException`, `ScosSecurityException`,
  `MethodNotImplementedException` — specialized cases.

Messages are not hardcoded — `ScosException` carries a `code` resolved from i18n
bundles (via `LocaleService`). Define a project `ExceptionCode` enum implementing
the foundation `ExceptionCode` contract, with message keys per code.

```java
public enum OrderExceptionCode implements ExceptionCode {
    ORDER_NOT_FOUND("order.not-found");
    private final String code;
    OrderExceptionCode(String code) { this.code = code; }
    @Override public String getCode() { return code; }
}
// throw: throw new ScosException(OrderExceptionCode.ORDER_NOT_FOUND, orderId);
```

## Lombok is the house standard (with one boundary)

The foundation uses Lombok throughout: `@Data`/`@Builder` on DTOs,
`@RequiredArgsConstructor` for injection, `@Getter`/`@ToString` on exceptions,
`@Slf4j`/`@Log4j2` for logging. Follow this for DTOs, services, configs, and
infrastructure. The one boundary: do NOT put `@Data`/`@Setter` on DDD aggregates
or value objects — that reintroduces the mutability the domain model avoids. Keep
aggregates hand-written; use records + behavior (see `ddd-tactical-design`).

## Cross-cutting foundation features (use them, don't reinvent)

Reach for the foundation instead of rolling your own — auditing (`@Auditable` +
`scos.audit.enabled=true`), idempotency (`@JdempotentResource` and friends),
caching (SCOS cache config + key generators), LGPD data masking (`@DataMask`,
`DataMaskingService`), Brazilian validators (`@CPF`, `@CNPJ`, `@ZipCode`,
`@TaxIdentifier`), current user (`ScosUserAuthentication`), i18n
(`LocaleService`), and JPA filtering/pagination (`SpecificationRepository`,
`ScosPaginationFilterDTO`). Details and imports in
`references/foundation-modules.md`.

## Config conventions

- `spring.main.allow-bean-definition-overriding: true` (the foundation relies on
  it).
- Hibernate JSON mapping via the foundation's `JacksonCustomJsonFormatMapper`.
- Liquibase owns the schema (`ddl-auto: validate`/`none`).

## Review — conformance checklist

Flag deviations from the house style, ordered by impact:
1. Raw `@RestController`/`@GetMapping` instead of `@ScosController`/`@ScosRequest*`.
2. A hand-written `@RestControllerAdvice` duplicating the foundation handler.
3. Raw `RuntimeException`/`ResponseStatusException` or hardcoded messages instead
   of `ScosException` + an `ExceptionCode` with i18n keys.
4. Services without the `specification` interface + `Bean` implementation split,
   or using field injection instead of `@RequiredArgsConstructor`.
5. Endpoints returning bare payloads instead of `ScosResponseDTO<T>` (and
   ad-hoc pagination instead of `ScosPaginatedDTO`/`ScosPaginationFilterDTO`).
6. Reinventing audit/idempotency/caching/masking/BR-validation the foundation
   already provides.

Output as: one-paragraph health summary, then impact-ordered findings with the
why and a minimal SCOS-aligned fix, then 1–3 things already done right.
