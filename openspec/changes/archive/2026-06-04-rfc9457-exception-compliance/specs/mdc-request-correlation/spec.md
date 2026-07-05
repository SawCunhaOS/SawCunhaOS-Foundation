## ADDED Requirements

### Requirement: X-Request-ID SHALL be populated in MDC for every request regardless of URI
O `LoggingInitialFilter` SHALL popular `X-Request-ID` e `IS_IP` no MDC para qualquer requisição, independente de a URI conter o padrão configurado (ex: `/api`). O logging detalhado (request/response body) SHALL permanecer condicional pela URI configurada. O header `X-Request-ID` SHALL ser ecoado na response para correlação cliente ↔ log.

#### Scenario: X-Request-ID populated for API URI
- **WHEN** request chega em `/api/users`
- **THEN** `MDC.get("X-Request-ID")` SHALL conter valor não nulo durante processamento da requisição

#### Scenario: X-Request-ID populated for non-API URI
- **WHEN** request chega em `/actuator/health`
- **THEN** `MDC.get("X-Request-ID")` SHALL conter valor não nulo durante processamento da requisição

#### Scenario: X-Request-ID echoed in response header
- **WHEN** qualquer request é processada pelo `LoggingInitialFilter`
- **THEN** a response SHALL conter o header `X-Request-ID` com o mesmo valor populado no MDC

### Requirement: X-Request-ID SHALL be derived from request header or generated as UUID
Quando o cliente envia o header `X-Request-ID`, o `LoggingInitialFilter` SHALL usar esse valor no MDC. Quando o header está ausente, SHALL gerar um UUID v4 e populá-lo no MDC e na response.

#### Scenario: Provided X-Request-ID is used from header
- **WHEN** request contém header `X-Request-ID: my-correlation-id`
- **THEN** `MDC.get("X-Request-ID")` SHALL ser igual a `"my-correlation-id"`
- **THEN** response header `X-Request-ID` SHALL ser igual a `"my-correlation-id"`

#### Scenario: Missing X-Request-ID generates a UUID
- **WHEN** request não contém header `X-Request-ID`
- **THEN** `MDC.get("X-Request-ID")` SHALL ser um UUID v4 válido (formato `xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx`)
- **THEN** response header `X-Request-ID` SHALL conter o mesmo UUID gerado

### Requirement: MDC SHALL be cleared after every request even when exceptions are thrown
O `LoggingInitialFilter` SHALL garantir `MDC.clear()` em bloco `finally`, independente de exceções na cadeia de filtros. Isso SHALL prevenir vazamento de contexto entre threads do pool. O `LoggingFinalFilter` NÃO SHALL ser responsável pelo `MDC.clear()` final.

#### Scenario: MDC is cleared after normal request
- **WHEN** request é processada com sucesso
- **THEN** após `doFilterInternal` retornar, `MDC.getCopyOfContextMap()` SHALL ser nulo ou vazio

#### Scenario: MDC is cleared even when chain throws exception
- **WHEN** a cadeia de filtros lança `RuntimeException` durante processamento
- **THEN** após propagação da exceção, `MDC.getCopyOfContextMap()` SHALL ser nulo ou vazio
- **THEN** a exceção SHALL ser re-propagada normalmente (não engolida)

### Requirement: MDC SHALL NOT leak Request-Content-Type or Request-Body after initial log
O `LoggingInitialFilter` SHALL remover `Request-Content-Type` e `Request-Body` do MDC após o log inicial, usando as mesmas chaves com que foram inseridos. Após o log inicial, apenas `X-Request-ID` e `IS_IP` SHALL permanecer no MDC durante o processamento.

#### Scenario: Request-Content-Type is removed after initial log
- **WHEN** `LoggingInitialFilter` executa em URI `/api/users` com `Content-Type: application/json`
- **THEN** após o log inicial e antes de `chain.doFilter()`, `MDC.get("Request-Content-Type")` SHALL ser nulo

#### Scenario: Request-Body is removed after initial log
- **WHEN** `LoggingInitialFilter` executa em URI `/api/users` com body não vazio
- **THEN** após o log inicial e antes de `chain.doFilter()`, `MDC.get("Request-Body")` SHALL ser nulo

#### Scenario: X-Request-ID persists after initial log
- **WHEN** `LoggingInitialFilter` executa o log inicial
- **THEN** `MDC.get("X-Request-ID")` SHALL permanecer não nulo durante toda a requisição

### Requirement: LoggingInitialFilter SHALL execute at most once per request
O `LoggingInitialFilter` SHALL estender `OncePerRequestFilter` para garantir execução única mesmo em cenários de `forward` ou `include` (ex: forward para `/error` pelo `BasicErrorController` do Spring). O mesmo SHALL aplicar ao `LoggingFinalFilter`.

#### Scenario: Filter executes once on normal request
- **WHEN** request é processada normalmente sem forwards
- **THEN** o corpo do filtro SHALL executar exatamente uma vez

#### Scenario: Filter does not re-execute on Spring error forward
- **WHEN** Spring faz `forward` para `/error` após exceção não tratada
- **THEN** o corpo do `LoggingInitialFilter` NÃO SHALL executar uma segunda vez para o mesmo request

### Requirement: requestId field in ProblemDetail SHALL reflect MDC X-Request-ID value
O `ExceptionsHandler` SHALL ler `MDC.get("X-Request-ID")` e incluí-lo como campo de extensão `requestId` no `ProblemDetail`. Quando o MDC não contiver `X-Request-ID`, o campo `requestId` SHALL ser omitido sem erro.

#### Scenario: requestId is populated from MDC in error response
- **WHEN** `ScosException` é lançada com `X-Request-ID: abc-123` no MDC
- **THEN** o `ProblemDetail` retornado SHALL conter `requestId` igual a `"abc-123"` em `properties`

#### Scenario: requestId is absent when MDC has no X-Request-ID
- **WHEN** `ScosException` é lançada sem `X-Request-ID` no MDC (ex: rota fora do filtro)
- **THEN** o `ProblemDetail` retornado NÃO SHALL conter o campo `requestId`
- **THEN** NÃO SHALL ocorrer exceção ou erro de serialização

#### Scenario: requestId is consistent with response header X-Request-ID
- **WHEN** request chega com header `X-Request-ID: my-id` e resulta em erro
- **THEN** o campo `requestId` no body SHALL ser igual a `"my-id"`
- **THEN** o header `X-Request-ID` na response SHALL ser igual a `"my-id"`
