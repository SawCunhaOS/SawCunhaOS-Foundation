## 1. Contratos e Tipos Compartilhados (módulo `exception`)

- [x] 1.1 Adicionar método `default URI getType()` na interface `ExceptionCode`, derivando URI a partir de `getCode()` (formato `https://docs.sawcunhaos.com.br/problems/<code-lowercase-hifenizado>`); documentar contrato com Javadoc descrevendo a regra de derivação e quando sobrescrever
- [x] 1.2 Adicionar método `default String getTitle()` na interface `ExceptionCode` retornando `"Error"`; documentar que implementadores devem sobrescrever para títulos específicos por categoria
- [x] 1.3 Sobrescrever `getTitle()` em cada constante relevante de `ScosExceptionCode` (ex: erros de validação → `"Validation Error"`, acesso negado → `"Access Denied"`, não encontrado → `"Not Found"`, erro interno → `"Internal Server Error"`); adicionar Javadoc em cada constante descrevendo o cenário de uso
- [x] 1.4 Criar record `ScosFieldError(String pointer, String detail)` no pacote do módulo `exception`; documentar com Javadoc que `pointer` segue RFC 6901 (ex: `"#/email"` para campo `email`) e `detail` é a mensagem de validação localizada
- [x] 1.5 Escrever `ScosExceptionCodeTest`: testar `getType()` com código sem override (verifica URI derivada), testar `getTitle()` com código sem override (verifica `"Error"`), testar `getTitle()` em `ATTRIBUTE_NOT_VALID` (verifica `"Validation Error"`)
- [x] 1.6 Escrever `ScosFieldErrorTest`: testar instanciação com campo simples (`pointer == "#/email"`), testar instanciação com campo aninhado (`address.street` → `pointer == "#/address/street"`)

## 2. ExceptionsHandler — Migração para ProblemDetail (módulo `exception`)

- [x] 2.1 Criar método `private ProblemDetail enrich(ProblemDetail problem)` no `ExceptionsHandler`: lê `MDC.get("X-Request-ID")` e, se não nulo, chama `problem.setProperty("requestId", requestId)`; sempre chama `problem.setProperty("timestamp", Instant.now().toString())`; documentar com Javadoc que este método centraliza o enriquecimento de correlação e rastreabilidade
- [x] 2.2 Migrar handler de `ScosException` para retornar `ResponseEntity<ProblemDetail>`: remover `@ResponseStatus`, construir `ProblemDetail.forStatus(status)`, popular `type` via `exception.getCode().getType()`, `title` via `exception.getCode().getTitle()`, `detail` via `LocaleService`, `instance` via `request.getRequestURI()`, `code` via `exception.getCode().getCode()`; chamar `enrich()`; documentar Javadoc com contrato de mapeamento de campos
- [x] 2.3 Migrar handler de `MethodArgumentNotValidException` para retornar `ResponseEntity<ProblemDetail>`: construir lista de `ScosFieldError` a partir de `ex.getBindingResult().getFieldErrors()` convertendo `field.getField()` para JSON Pointer (`"#/" + field`); documentar Javadoc que `pointer` segue RFC 6901 e a regra de conversão para campos aninhados
- [x] 2.4 Migrar demais handlers em `ExceptionsHandler` (404 Not Found, 405 Method Not Allowed, 500 Internal Server Error, etc.) para retornar `ResponseEntity<ProblemDetail>`; remover todos os `@ResponseStatus` dos métodos; garantir que `enrich()` é chamado em todos
- [x] 2.5 Remover a classe `ExceptionResponse` após migração completa de todos os handlers; remover todas as referências a `ScosResponseDTO<ExceptionResponse>`
- [x] 2.6 Escrever `ExceptionsHandlerScosExceptionTest`: testar `handleScosException` com `ScosException` de negócio → verificar status 400, `Content-Type: application/problem+json`, `detail` localizado, `code`, `type`, `title`, `instance`, ausência do wrapper `data`; documentar cenário com Javadoc no método de teste
- [x] 2.7 Escrever `ExceptionsHandlerValidationTest`: testar `handleMethodArgumentNotValidException` com campos simples → verificar `errors[].pointer` com prefixo `#/` e `errors[].detail` com mensagem; testar campo aninhado → verificar JSON Pointer aninhado; testar múltiplos campos → verificar todos os itens em `errors[]`
- [x] 2.8 Escrever `ExceptionsHandlerInternalErrorTest`: testar handler de exceção genérica → verificar status 500, `title` "Internal Server Error", ausência de stack trace no `detail`
- [x] 2.9 Escrever `ExceptionsHandlerMdcTest`: testar com `X-Request-ID` no MDC → verificar `requestId` presente no `ProblemDetail`; testar sem `X-Request-ID` no MDC → verificar ausência de `requestId` sem erro; testar `timestamp` presente em ISO-8601 em todos os cenários
- [x] 2.10 Escrever `ExceptionsHandlerAccessDeniedTest`: migrar `AccessDeniedExceptionHandler` para retornar `ResponseEntity<ProblemDetail>`; escrever testes verificando status 403, `type`, `title`, `code` e `Content-Type: application/problem+json`

## 3. ExceptionHandlerFilter — Migração para ProblemDetail (módulo `security`)

- [x] 3.1 Injetar `ObjectMapper` no `ExceptionHandlerFilter` (via construtor ou `@Autowired`); substituir serialização de `ExceptionResponse` por `ProblemDetail`; documentar Javadoc que o filtro opera fora do `DispatcherServlet` e por isso serializa manualmente
- [x] 3.2 Definir `response.setContentType("application/problem+json")` antes de escrever o body no `ExceptionHandlerFilter`; garantir que o `ProblemDetail` gerado segue o mesmo padrão do `ExceptionsHandler` (mesmos campos, `enrich()` equivalente)
- [x] 3.3 Escrever `ExceptionHandlerFilterTest`: testar captura de `ScosSecurityException` → verificar body com `ProblemDetail` válido, status 403, `Content-Type: application/problem+json`; usar `MockHttpServletRequest` e `MockHttpServletResponse` sem Spring context

## 4. LoggingInitialFilter — Correções MDC (módulo `utils`)

- [x] 4.1 Migrar `LoggingInitialFilter` de `implements Filter` para `extends OncePerRequestFilter`; substituir `doFilter()` por `doFilterInternal()`; documentar Javadoc que `OncePerRequestFilter` previne dupla execução em forwards do Spring (ex: forward para `/error`)
- [x] 4.2 Separar setup do MDC base (fora do condicional de URI): mover `MDC.put("X-Request-ID", ...)` e `MDC.put("IS_IP", ...)` para antes do bloco `if (uri.contains(...))`; adicionar `response.setHeader("X-Request-ID", MDC.get("X-Request-ID"))` no mesmo bloco universal; documentar Javadoc que `X-Request-ID` e `IS_IP` devem estar disponíveis para qualquer rota, incluindo `/actuator` e `/health`
- [x] 4.3 Implementar lógica de resolução de `X-Request-ID`: se request contém header `X-Request-ID`, usar o valor; senão, gerar `UUID.randomUUID().toString()`; extrair em método `private String resolveRequestId(HttpServletRequest request)` com Javadoc
- [x] 4.4 Corrigir chaves erradas em `MDC.remove()`: substituir `MDC.remove("Content-Type")` por `MDC.remove("Request-Content-Type")` e `MDC.remove("Body")` por `MDC.remove("Request-Body")`; documentar no Javadoc do método ou inline que a inconsistência anterior causava vazamento dessas chaves no MDC
- [x] 4.5 Envolver `chain.doFilter()` em bloco `try/finally` com `MDC.clear()` no `finally`; remover qualquer `MDC.clear()` que existia fora do `finally`
- [x] 4.6 Escrever `LoggingInitialFilterTest` — cenário: `X-Request-ID` presente no header → verificar que MDC contém o valor durante a chain e que response header contém o mesmo valor; usar `MockFilterChain` com spy para capturar estado do MDC durante execução
- [x] 4.7 Escrever `LoggingInitialFilterTest` — cenário: `X-Request-ID` ausente no header → verificar que MDC contém UUID v4 válido durante a chain
- [x] 4.8 Escrever `LoggingInitialFilterTest` — cenário: URI `/actuator/health` (fora do padrão `/api`) → verificar que `X-Request-ID` está no MDC durante a chain (validação da separação do MDC base)
- [x] 4.9 Escrever `LoggingInitialFilterTest` — cenário: após log inicial em `/api/users` → verificar que `Request-Content-Type` e `Request-Body` estão ausentes do MDC quando a chain executa (valida correção das chaves de remove)
- [x] 4.10 Escrever `LoggingInitialFilterTest` — cenário: chain lança `RuntimeException` → verificar que MDC está vazio após propagação da exceção (valida `finally`); verificar que a exceção é re-propagada

## 5. LoggingFinalFilter — Correções (módulo `utils`)

- [x] 5.1 Migrar `LoggingFinalFilter` de `implements Filter` para `extends OncePerRequestFilter`; substituir `doFilter()` por `doFilterInternal()`
- [x] 5.2 Remover chamada a `MDC.clear()` do `LoggingFinalFilter` (responsabilidade migrou para `LoggingInitialFilter`)
- [x] 5.3 Escrever `LoggingFinalFilterTest`: testar URI `/api/users` → verificar que `MDC.get("Response-Code")` contém o status durante o log; testar URI `/actuator` → verificar que MDC não é populado com campos de response

## 6. Documentação e Convenções

- [x] 6.1 Atualizar `scos-conventions/SKILL.md` — seção de erros: substituir referência a `ExceptionResponse` por `ProblemDetail`; atualizar exemplos de response de erro para o novo formato RFC 9457; esclarecer que `ScosResponseDTO` é para sucesso (`2xx`) e `ProblemDetail` é para erros (`4xx`/`5xx`)
- [x] 6.2 Atualizar `spring-boot-service/references/rest-and-errors.md` — remover a proibição explícita de `ProblemDetail`; atualizar a descrição do `ExceptionsHandler` para refletir retorno de `ResponseEntity<ProblemDetail>`; adicionar exemplo do novo formato de response de erro
- [x] 6.3 Escrever entrada no `CHANGELOG` com: (a) tabela de mapeamento campo-a-campo `data.message` → `detail`, `data.codeError` → `code`, `data.validationErrors[].attribute` → `errors[].pointer`, `data.validationErrors[].message` → `errors[].detail`; (b) nota sobre `Content-Type` mudando para `application/problem+json`; (c) campos novos adicionados: `type`, `title`, `status`, `instance`, `requestId`, `timestamp`; (d) remoção do wrapper `data`

## Notas de implementação (desvios das tasks)

- **`enrich` centralizado**: em vez de duplicar a lógica, `enrich`/builders vivem em
  `ScosProblemDetails` (módulo `exception`). `ExceptionsHandler.enrich()` (privado,
  conforme task 2.1) delega para `ScosProblemDetails.enrich()`; os handlers de
  segurança chamam `ScosProblemDetails.enrich()` diretamente (task 3.2 "equivalente").
- **`ScosException.getCode()` é `String`** (não `ExceptionCode`). Tasks 2.2/2.3
  assumiam `getCode().getType()`. Para `ScosException` o `type` é derivado do código
  string (mesma regra de `ExceptionCode.getType()`) e o `title` é `"Business Error"`.
  Handlers de validação usam o enum `ScosExceptionCode` diretamente (com `getType()`/`getTitle()`).
- **Status do `ExceptionHandlerFilter` (tasks 3.3/2.10)**: o filtro de segurança
  captura `ScosSecurityException` (falhas de autenticação) → **401 Unauthorized**,
  preservando o comportamento anterior. O **403** RFC 9457 fica com o
  `AccessDeniedExceptionHandler` (Spring `AccessDeniedException`) e com
  `ExceptionsHandler.handleAccessDeniedException` (nio). Testes ajustados a 401/403
  conforme o handler.
- **Tasks 6.1/6.2 (skills `scos-conventions`/`spring-boot-service`)**: esses arquivos
  não existem neste repositório (são skills externas/agentes). Atualizei a doc de
  convenção equivalente presente no repo (`README.md`, seção Exception). Os SKILL.md
  devem ser atualizados no repositório das skills.
- **Pré-existente**: `audit/AuditIntegrationTest` falha no ambiente sem banco/Docker
  (não relacionado a esta mudança). Os 24 testes novos passam.
- **4º ponto de erro**: além dos 3 handlers, `AuthorizationRequiredFilter` também
  usava `ExceptionResponse` — migrado para `ProblemDetail` (401) para permitir a
  remoção completa de `ExceptionResponse`.
