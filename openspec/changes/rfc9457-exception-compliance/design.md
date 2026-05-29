## Context

O módulo `exception` da foundation retorna erros via `ScosResponseDTO<ExceptionResponse>` com wrapper `data`, violando a RFC 9457 (Problem Details for HTTP APIs). Existem três handlers com formatos distintos:

- `ExceptionsHandler` → `ScosResponseDTO<ExceptionResponse>` (com wrapper)
- `AccessDeniedExceptionHandler` → `ExceptionResponse` raw (sem wrapper)
- `ExceptionHandlerFilter` → `ExceptionResponse` raw (sem wrapper)

O módulo `utils` possui dois filtros de logging com bugs confirmados em `MDC.remove()` que causam vazamento de campos no MDC. O Spring Framework 7 (Spring Boot 4) oferece `ProblemDetail` como suporte nativo à RFC 9457, eliminando a necessidade de manter `ExceptionResponse` próprio.

## Goals / Non-Goals

**Goals:**
- Respostas de erro RFC 9457 compliant em todos os handlers
- Preservar todas as informações atuais retornadas — apenas renomear e reposicionar campos
- Unificar os três handlers sob o mesmo formato `ProblemDetail`
- Corrigir bugs do MDC (`MDC.remove()` com chaves erradas, execução dupla em forwards, `MDC.clear()` sem `finally`)
- Adicionar `requestId` e `timestamp` como extensões para rastreabilidade
- Cobertura de testes unitários para todos os comportamentos alterados

**Non-Goals:**
- Alterar formato de respostas de sucesso (`ScosResponseDTO` permanece para `2xx`)
- Modificar o mecanismo de `throw` via `ScosException` (API dos serviços inalterada)
- Criar endpoints versionados ou período de transição com duplo formato
- Propagação de MDC para threads assíncronas (`@Async`) nesta iteração — identificado como melhoria futura

## Decisions

### D1 — Usar `ProblemDetail.forStatus()` + `setProperty()` em vez de subclassificar

**Escolhido**: `ProblemDetail.forStatus(status)` + `problem.setProperty("code", ...)` para campos de extensão SCOS.

**Alternativa rejeitada**: `class ScosProblemDetail extends ProblemDetail`. Subclasses com getters próprios produzem campos duplicados no JSON — o pai serializa via `@JsonAnyGetter` do mapa `properties`, e o filho serializa via getter próprio. Comportamento indefinido e difícil de debugar.

**Impacto**: Todos os handlers constroem `ProblemDetail` diretamente via factory method. Método `enrich(ProblemDetail)` centraliza a adição de `requestId` e `timestamp`.

### D2 — Enriquecer `ExceptionCode` com `getType()` e `getTitle()` default em vez de `ScosProblemType` separado

**Escolhido**: Adicionar métodos `default` na interface `ExceptionCode`:
```java
default URI getType() {
    return URI.create("https://docs.sawcunhaos.com.br/problems/" + getCode().toLowerCase().replace("_", "-"));
}
default String getTitle() { return "Error"; }
```

**Alternativa rejeitada**: Enum `ScosProblemType` paralelo. Criaria dois sistemas de código de erro desconexos. Implementadores existentes de `ExceptionCode` precisariam de mapeamento manual entre os dois.

**Impacto**: Zero breaking change — métodos `default` são opcionais. `ScosExceptionCode` pode sobrescrever para valores mais específicos (ex: `ATTRIBUTE_NOT_VALID.getTitle() = "Validation Error"`).

### D3 — `MDC.clear()` em `finally` no `LoggingInitialFilter` (`@Order(0)`)

**Escolhido**: Mover `MDC.clear()` para bloco `finally` do `LoggingInitialFilter` (primeiro filtro da cadeia). `LoggingFinalFilter` deixa de chamar `MDC.clear()` — responsabilidade migra para o filtro de mais alta prioridade.

**Rationale**: Garante limpeza mesmo se exceção escapar da cadeia inteira. `@Order(0)` envolve toda a cadeia; `@Order(100)` pode não ser alcançado em cenários de erro na security layer.

### D4 — `OncePerRequestFilter` para ambos os filtros de logging

**Escolhido**: Migrar `LoggingInitialFilter` e `LoggingFinalFilter` de `implements Filter` para `extends OncePerRequestFilter`.

**Rationale**: Spring faz `forward` para `/error` em exceções não tratadas. Com `Filter` raw, ambos os filtros executam duas vezes. `OncePerRequestFilter` usa atributo de request como flag para garantir execução única.

### D5 — `X-Request-ID` populado fora do condicional de URI

**Escolhido**: Separar MDC base (universal) do logging detalhado (condicional por URI):
```java
// Sempre — independente da URI:
MDC.put("X-Request-ID", resolveRequestId(req));
MDC.put("IS_IP", resolveClientIp(req));
response.setHeader("X-Request-ID", MDC.get("X-Request-ID"));

// Condicional — apenas URIs configuradas (ex: /api):
if (req.getRequestURI().contains(scosFilterProperties.getURI())) {
    // log detalhado de request/response
}
```

**Rationale**: `ExceptionHandlerFilter` captura erros de segurança em rotas `/actuator`, `/health` etc. Sem MDC universal, `ProblemDetail` não consegue incluir `requestId` nesses casos.

### D6 — Renomear `FieldError` para `ScosFieldError`

**Escolhido**: `public record ScosFieldError(String pointer, String detail) {}`

**Rationale**: `FieldError` já existe em `org.springframework.validation`. Em classes que usam ambos (como `ExceptionsHandler` que processa `MethodArgumentNotValidException`), o conflito de import força fully-qualified name — código ilegível. Prefixo `Scos` resolve sem ambiguidade.

## Risks / Trade-offs

**[Breaking change para consumidores]** → Documentar mapeamento campo-a-campo no CHANGELOG. Serviços devem ser versionados antes de atualizar a foundation. Campos mapeados 1:1 — nenhuma informação é perdida.

**[Content-Type `application/problem+json` pode rejeitar clientes antigos]** → Clientes que validam `Content-Type: application/json` exato quebram. Mitigação: Spring inclui `application/problem+json` como compatível com `application/*+json`. Maioria dos clientes HTTP ignora sufixo `+json`.

**[`ExceptionHandlerFilter` serializa JSON manualmente (security layer)]** → Filtro não tem acesso ao `HttpMessageConverter` do Spring. Solução: injetar `ObjectMapper` existente e serializar `ProblemDetail` diretamente, definindo `Content-Type` no response antes de escrever.

**[Testes do `LoggingInitialFilter` validam MDC dentro da chain]** → Após `doFilter()` retornar, `finally` limpa o MDC. Testes que verificam MDC após a execução sempre veem MDC vazio. Solução: usar `FilterChain` mock com captor que captura o estado do MDC durante a execução, antes do `finally`.

## Migration Plan

1. Implementar alterações em `exception`, `security`, `utils` na foundation
2. Atualizar `scos-conventions/SKILL.md` e `spring-boot-service/references/rest-and-errors.md`
3. Publicar nova versão com bump minor (breaking change) e CHANGELOG com tabela de mapeamento de campos
4. Serviços que consomem respostas de erro devem atualizar o parsing: `data.message` → `detail`, `data.codeError` → `code`, `data.validationErrors[].attribute` → `errors[].pointer` (prefixar `#/`), `data.validationErrors[].message` → `errors[].detail`

**Rollback**: Reverter para versão anterior da foundation. Não há estado persistido — mudança é apenas no formato da response.
