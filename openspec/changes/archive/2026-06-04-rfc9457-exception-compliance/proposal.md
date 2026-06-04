## Why

O módulo `exception` retorna erros em formato proprietário (`ScosResponseDTO<ExceptionResponse>`) incompatível com RFC 9457, e possui três padrões distintos de resposta de erro no mesmo sistema. RFC 9457 é o padrão consolidado no mercado (adotado por Spring 6+, Quarkus, Micronaut), e a adoção agora alinha a foundation com Spring Boot 4 / Spring Framework 7, que oferece suporte nativo via `ProblemDetail`.

## What Changes

- **BREAKING** Substituir `ScosResponseDTO<ExceptionResponse>` por `ProblemDetail` nativo do Spring 7 em todas as respostas de erro
- **BREAKING** Remover wrapper `data` das respostas de erro (`data.message` → `detail`, `data.codeError` → `code`, `data.validationErrors[].attribute` → `errors[].pointer`, `data.validationErrors[].message` → `errors[].detail`)
- **BREAKING** `Content-Type` muda de `application/json` para `application/problem+json` em respostas de erro
- Unificar três handlers de erro (`ExceptionsHandler`, `AccessDeniedExceptionHandler`, `ExceptionHandlerFilter`) sob o mesmo formato `ProblemDetail`
- Enriquecer `ExceptionCode` com métodos `default getType()` e `getTitle()` (sem breaking change nos implementadores)
- Adicionar campos de extensão `requestId` (do MDC `X-Request-ID`) e `timestamp` (ISO-8601 UTC) em todas as respostas de erro
- Corrigir bugs no `LoggingInitialFilter`: chaves erradas em `MDC.remove()`, migrar para `OncePerRequestFilter`, garantir `MDC.clear()` em bloco `finally`, separar MDC base (universal) do logging condicional
- Ecoar header `X-Request-ID` na response para correlação cliente ↔ log
- Renomear `FieldError` para `ScosFieldError` (evitar conflito com `org.springframework.validation.FieldError`)
- Atualizar convenções: `scos-conventions/SKILL.md` e `spring-boot-service/references/rest-and-errors.md`

## Capabilities

### New Capabilities

- `rfc9457-error-response`: Respostas de erro RFC 9457 compliant com `ProblemDetail` nativo — campos `type`, `title`, `status`, `detail`, `instance`, `code`, `errors` (JSON Pointer), `requestId`, `timestamp`; `Content-Type: application/problem+json`; unificação dos três handlers
- `mdc-request-correlation`: MDC correto e completo — correção dos bugs de `MDC.remove()`, migração para `OncePerRequestFilter`, `MDC.clear()` em `finally`, `X-Request-ID` universal (fora do filtro condicional), eco do header na response, propagação para threads assíncronas via `TaskDecorator`

### Modified Capabilities

<!-- Nenhuma spec existente afetada — foundation não possui specs criadas anteriormente -->

## Impact

**Módulos afetados:**
- `exception`: `ExceptionsHandler`, `AccessDeniedExceptionHandler`, `ExceptionResponse`, `ExceptionCode`, `ScosExceptionCode` — reescrita completa do contrato de resposta
- `security`: `ExceptionHandlerFilter` — migrar para `ProblemDetail` com `Content-Type: application/problem+json`
- `utils`: `LoggingInitialFilter`, `LoggingFinalFilter` — correções de bugs MDC e migração para `OncePerRequestFilter`

**APIs afetadas:**
- Todos os endpoints que retornam erros `4xx`/`5xx` — breaking change no formato do body
- Serviços que consomem `data.message`, `data.codeError`, `data.validationErrors` precisam ser atualizados

**Dependências:**
- Sem novas dependências — `ProblemDetail` é nativo do Spring Framework 7 (já no BOM)
- `OncePerRequestFilter` é nativo do Spring Web (já presente)

**Testes:**
- Testes unitários novos: `LoggingInitialFilterTest`, `LoggingFinalFilterTest`, `ExceptionsHandlerTest`, `ExceptionsHandlerMdcTest`
- Infraestrutura: JUnit + Mockito + `spring-test` com `MockHttpServletRequest`/`MockHttpServletResponse`/`MockFilterChain` — sem Spring context
- Cada comportamento documentado com Javadoc descrevendo contrato e invariantes
