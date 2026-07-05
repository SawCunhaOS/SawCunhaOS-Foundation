# REST layer, error handling & review rubric

Keep the web layer thin: DTOs in, `ScosResponseDTO` out, one application call per
endpoint, errors thrown as `ScosException`. This follows the SCOS house style
(see the `scos-conventions` skill) on Spring Boot 4.0.x / Spring Framework 7.

## Controllers & DTOs (SCOS style)

DTOs are records in `api.dto`. Never accept or return domain aggregates or JPA
entities at the boundary. Use `@ScosController` + `@ScosRequest*` and wrap the
payload in `ScosResponseDTO<T>`. Map with MapStruct.

```java
@ScosController
class OrderController {

    private final OrderService orderService;   // specification interface
    private final OrderApiMapper mapper;

    @ScosRequestPOST(uri = "/orders", httpCode = HttpStatus.CREATED)
    ScosResponseDTO<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
        var orderId = orderService.place(mapper.toCommand(request));   // delegate inward
        return ScosResponseDTO.<OrderResponse>builder()
            .data(mapper.toResponse(orderId))
            .build();
    }
}

record PlaceOrderRequest(
    @NotNull UUID customerId,
    @NotEmpty List<@Valid OrderLineRequest> lines) {}

record OrderLineRequest(@NotNull UUID productId, @Min(1) int quantity) {}
```

`@ScosController` already supplies `@RestController`, the `/api` base path, and
JSON; `@ScosRequestPOST` supplies the method and status. Business rules stay in
the domain; the controller only validates shape and delegates.

## Error handling — throw ScosException (no custom advice)

Do NOT write a `@RestControllerAdvice`/`ProblemDetail` handler. The foundation's
`ExceptionsHandler` is already global and returns a localized `ExceptionResponse`
(`message`, `codeError`, `validationErrors`). Signal errors by throwing
`ScosException(ExceptionCode, args...)` (or the specialized
`ScosNoContentException` / `ScosNoRollbackException` / `ScosSecurityException`)
from the application layer. Messages come from i18n bundles keyed by the code.

```java
public enum OrderExceptionCode implements ExceptionCode {
    ORDER_NOT_FOUND("order.not-found"),
    ORDER_ALREADY_CONFIRMED("order.already-confirmed");
    private final String code;
    OrderExceptionCode(String code) { this.code = code; }
    @Override public String getCode() { return code; }
}

// in the application service:
var order = orders.findById(id)
    .orElseThrow(() -> new ScosException(OrderExceptionCode.ORDER_NOT_FOUND, id.value()));
```

Bean Validation errors are also handled by the foundation handler and surfaced in
`validationErrors`; apply `@Valid` and the BR validators (`@CPF`, `@CNPJ`,
`@ZipCode`) to request DTOs.

## API versioning (Spring 7)

When you need versions, use the framework's built-in API versioning (negotiated
by path, `Accept-Version` header, or query param) rather than custom filters. It
generates clean OpenAPI deprecation metadata and correct 404/406 responses.
Configure the strategy centrally and annotate endpoints with their version.

## Review rubric

Order findings by impact.

### 1. Layering & leaks (highest impact)
- Do controllers contain business logic, or only validation + delegation?
- Are domain aggregates or JPA entities exposed directly in requests/responses
  instead of DTOs?
- Is `open-in-view` left on (the default trap), causing lazy loading in the web
  layer?

### 2. Error handling (SCOS)
- Are errors thrown as `ScosException` + an `ExceptionCode`, letting the global
  foundation `ExceptionsHandler` respond — rather than a hand-rolled
  `@RestControllerAdvice`/`ProblemDetail` that duplicates it?
- Are messages i18n keys (resolved by `LocaleService`), not hardcoded strings?
- Do error responses leak stack traces or internal details?

### 3. Configuration & secrets
- Are secrets/environment values externalized (env vars) rather than committed?
- Are profiles used for environment differences?
- Is the schema owned by Liquibase with `ddl-auto: validate`/`none`, not
  `update`?

### 4. Validation & contracts
- Is Bean Validation applied to request DTOs (`@Valid`, constraints)?
- Are status codes and response shapes consistent across endpoints?

### 5. Observability & ops
- Are Actuator health probes exposed for liveness/readiness?
- Is the API versioned with the framework mechanism if it needs versioning?

### Output format for a review
1. **Health summary** — one paragraph: is the web layer thin and consistent?
2. **Findings** — impact-ordered, each with why + minimal fix.
3. **What's solid** — 1–3 things worth keeping.
