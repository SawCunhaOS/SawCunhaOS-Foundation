# Ideia: Conformidade do Módulo Exception com RFC 9457

## Contexto

A RFC 9457 (Problem Details for HTTP APIs) define um formato padronizado para respostas de erro em APIs HTTP. O módulo `exception` atual não atende essa especificação.

---

## Diagnóstico: O que está faltando

### Estrutura atual

```json
{
  "data": {
    "message": "Campo inválido em /api/users",
    "codeError": "SCOS-001",
    "validationErrors": [
      { "attribute": "email", "message": "deve ser um e-mail válido" }
    ]
  },
  "scosPaginatedDTO": null
}
```

### Estrutura exigida pela RFC 9457

```json
{
  "type": "https://docs.sawcunhaos.com.br/problems/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos.",
  "instance": "/api/users",
  "code": "SCOS-001",
  "errors": [
    {
      "pointer": "#/email",
      "detail": "deve ser um e-mail válido"
    }
  ]
}
```

### Gaps identificados

| Campo RFC 9457 | Status atual | Observação |
|---|---|---|
| `type` | ❌ Ausente | URI que identifica o tipo de problema |
| `title` | ❌ Ausente | Resumo curto e fixo do tipo de problema |
| `status` | ❌ Ausente | Código HTTP inteiro refletido no body |
| `detail` | ⚠️ Parcial | Presente como `message`, nome errado |
| `instance` | ❌ Ausente | URI da requisição que causou o erro |
| `Content-Type` | ❌ Ausente | Deve ser `application/problem+json` |
| Wrapper `data` | ❌ Violação | RFC exige root object, não wrapper |
| `validationErrors.attribute` | ⚠️ Parcial | Deve usar JSON Pointer (`#/email`) |

---

### Mapeamento: campos atuais → RFC 9457

Nenhuma informação é perdida. Todos os dados retornados hoje são preservados — apenas renomeados, reposicionados ou reformatados.

| Campo atual | Localização atual | Campo RFC 9457 | Localização nova | Transformação |
|---|---|---|---|---|
| `data.message` | wrapper `data` | `detail` | raiz | renomeado |
| `data.codeError` | wrapper `data` | `code` | raiz (extensão) | renomeado |
| `data.validationErrors[].attribute` | wrapper `data` | `errors[].pointer` | raiz (extensão) | `"email"` → `"#/email"` (JSON Pointer) |
| `data.validationErrors[].message` | wrapper `data` | `errors[].detail` | raiz (extensão) | renomeado |
| *(ausente)* | — | `type` | raiz | URI do tipo de problema |
| *(ausente)* | — | `title` | raiz | título fixo por tipo |
| *(ausente)* | — | `status` | raiz | HTTP status code refletido no body |
| *(ausente)* | — | `instance` | raiz | URI da requisição (`/api/users`) |
| *(ausente — MDC)* | MDC `X-Request-ID` | `requestId` | raiz (extensão) | correlação log ↔ erro |
| *(ausente)* | — | `timestamp` | raiz (extensão) | ISO-8601, prática de mercado para rastreabilidade |

> **Nota sobre `attribute` → JSON Pointer**: a conversão é mecânica — prefixar `#/` ao nome do campo. Para campos aninhados: `address.street` → `#/address/street`. Clientes que dependem do valor exato de `attribute` precisam atualizar o parse.

### Inconsistências adicionais

- `ExceptionsHandler`: retorna `ScosResponseDTO<ExceptionResponse>` (com wrapper)
- `AccessDeniedExceptionHandler`: retorna `ExceptionResponse` direto (sem wrapper)
- `ExceptionHandlerFilter`: retorna `ExceptionResponse` direto (sem wrapper)
- Três padrões distintos de resposta de erro no mesmo sistema

---

## Proposta de Solução

### 1. Adotar `ProblemDetail` nativo do Spring 7

Spring Framework 7 (Spring Boot 4) possui suporte nativo à RFC 9457 via `ProblemDetail`. O `ResponseEntityExceptionHandler` já retorna `ProblemDetail` por padrão desde Spring 6.

**Benefício**: não precisamos manter nosso próprio model de erro para os casos padrão do Spring.

### 2. Substituir `ExceptionResponse` por `ScosProblemDetail`

Criar um record/classe que estende ou encapsula `ProblemDetail`, adicionando os campos proprietários SCOS:

```java
// Ideia de estrutura
public class ScosProblemDetail extends ProblemDetail {
    private String code;       // ex: "SCOS-001"
    private List<FieldError> errors; // para erros de validação
}

public record FieldError(
    String pointer,  // JSON Pointer: "#/email"
    String detail    // mensagem de validação
) {}
```

### 3. Remover o wrapper `ScosResponseDTO` nas respostas de erro

Erros não devem ser envoltos em `{ "data": ... }`. O `ScosResponseDTO` faz sentido para respostas de sucesso, mas viola a RFC 9457 para erros.

**Estratégia**: `ExceptionsHandler` retorna `ResponseEntity<ProblemDetail>` diretamente.

### 4. Definir URIs para tipos de problema

Criar um enum ou constantes com as URIs dos tipos de problema:

```java
public enum ScosProblemType {
    VALIDATION_ERROR("https://docs.sawcunhaos.com.br/problems/validation-error", "Validation Error"),
    BUSINESS_ERROR("https://docs.sawcunhaos.com.br/problems/business-error", "Business Error"),
    ACCESS_DENIED("https://docs.sawcunhaos.com.br/problems/access-denied", "Access Denied"),
    NOT_FOUND("https://docs.sawcunhaos.com.br/problems/not-found", "Not Found"),
    INTERNAL_ERROR("https://docs.sawcunhaos.com.br/problems/internal-error", "Internal Server Error");
}
```

> URIs não precisam existir inicialmente, mas devem apontar para documentação futura.

### 5. Garantir `Content-Type: application/problem+json`

Spring configura isso automaticamente quando `ProblemDetail` é retornado via `ResponseEntity`. Verificar que o `ExceptionHandlerFilter` (security) também define o header correto.

### 6. Unificar os três pontos de resposta de erro

| Handler | Atual | Proposto |
|---|---|---|
| `ExceptionsHandler` | `ScosResponseDTO<ExceptionResponse>` | `ProblemDetail` |
| `AccessDeniedExceptionHandler` | `ExceptionResponse` raw | `ProblemDetail` |
| `ExceptionHandlerFilter` | `ExceptionResponse` raw | `ProblemDetail` |

---

## Exemplo de resposta final

Todos os campos do response atual estão presentes — renomeados ou reposicionados. Os campos `requestId` e `timestamp` são adições que completam o padrão de mercado.

### Erro de validação (400)
```json
{
  "type": "https://docs.sawcunhaos.com.br/problems/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos.",
  "instance": "/api/users",
  "code": "SCOS-001",
  "errors": [
    { "pointer": "#/email", "detail": "deve ser um e-mail válido" },
    { "pointer": "#/name", "detail": "não deve estar em branco" }
  ],
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-05-29T14:32:10.123Z"
}
```

### Erro de negócio (400)
```json
{
  "type": "https://docs.sawcunhaos.com.br/problems/business-error",
  "title": "Business Error",
  "status": 400,
  "detail": "CPF informado é inválido.",
  "instance": "/api/persons",
  "code": "SCOS-007",
  "requestId": "550e8400-e29b-41d4-a716-446655440001",
  "timestamp": "2026-05-29T14:32:10.456Z"
}
```

### Acesso negado (403)
```json
{
  "type": "https://docs.sawcunhaos.com.br/problems/access-denied",
  "title": "Access Denied",
  "status": 403,
  "detail": "Você não tem permissão para acessar este recurso.",
  "instance": "/api/admin/users",
  "code": "SCOS-004",
  "requestId": "550e8400-e29b-41d4-a716-446655440002",
  "timestamp": "2026-05-29T14:32:10.789Z"
}
```

---

## Impacto e Riscos

### Breaking changes
- Clientes que leem `data.message` → campo agora é `detail` na raiz
- Clientes que leem `data.codeError` → campo agora é `code` na raiz
- Clientes que leem `data.validationErrors[].attribute` → campo agora é `errors[].pointer` com prefixo `#/`
- Clientes que leem `data.validationErrors[].message` → campo agora é `errors[].detail`
- Wrapper `data` removido: `scosPaginatedDTO` some das respostas de erro (nunca foi útil em erros)
- `Content-Type` muda de `application/json` para `application/problem+json`

### O que não é perdido (todos os dados preservados)
- Mensagem de erro: `data.message` → `detail`
- Código de erro: `data.codeError` → `code`
- Lista de erros de validação: `data.validationErrors` → `errors` (reformat apenas)
- Campo do erro: `data.validationErrors[].attribute` → `errors[].pointer` (prefixar `#/`)
- Mensagem do campo: `data.validationErrors[].message` → `errors[].detail`
- Adições: `type`, `title`, `status`, `instance`, `requestId`, `timestamp`

### Mitigação
- Versionar a API dos serviços que usam a foundation antes de atualizar
- Comunicar mapeamento campo-a-campo no CHANGELOG da foundation
- Considerar período de transição com ambos os formatos via feature flag ou header `Accept`

### Não é breaking para
- Clientes que apenas verificam o HTTP status code
- Testes que validam `response.status` mas não o body

---

---

## Impacto na Geração de Controllers via Swagger (Mustache)

### Conclusão: impacto indireto

Os templates Mustache geram apenas o **caminho feliz** (happy path):
- `api.mustache` → interface com assinaturas de método e `@ScosRequest*`
- `apiController.mustache` → classe que delega para `Delegate`
- `apiDelegate.mustache` → lança `MethodNotImplementedException`
- `responseType.mustache` → tipo de retorno do método (ex: `ScosResponseDTO<UserDTO>`)

Respostas de erro **não passam pelo código gerado** — fluem pelo `@ControllerAdvice` (`ExceptionsHandler`). Portanto, a adoção de RFC 9457 **não quebra os templates nem o código gerado**.

### Impacto indireto: schemas OpenAPI nos serviços

Se equipes definem schemas de erro no `components/schemas` do YAML (ex: `ExceptionResponse`), esses modelos gerados ficarão desatualizados. Solução: remover schemas de erro proprietários dos YAMLs e referenciar o tipo `application/problem+json` como media type nas responses de erro:

```yaml
# Antes (proprietário)
responses:
  '400':
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/ExceptionResponse'

# Depois (RFC 9457)
responses:
  '400':
    content:
      application/problem+json:
        schema:
          $ref: '#/components/schemas/ProblemDetail'
```

O schema `ProblemDetail` pode ser definido em um arquivo OpenAPI compartilhado da foundation.

### Nenhuma mudança necessária nos templates

Os templates atuais não precisam ser alterados para suportar RFC 9457.

---

## Suporte a MDC (Mapped Diagnostic Context)

### MDC já existe na foundation

Há dois filtros implementados em `utils`:

| Filtro | Order | O que faz |
|---|---|---|
| `LoggingInitialFilter` | `@Order(0)` | Popula MDC com `X-Request-ID`, `IS_IP`, e campos de request; loga "Initial API Call" |
| `LoggingFinalFilter` | `@Order(100)` | Popula MDC com campos de response; loga "Final API Call"; chama `MDC.clear()` |

`ScosHibernateAuditListener` já consome `MDC.get("IS_IP")` e `MDC.get("X-Request-ID")` para auditoria.

---

### Validação da implementação atual

#### O que deveria persistir vs. o que realmente persiste

`LoggingInitialFilter` tenta remover a maioria dos campos imediatamente após o log inicial, mantendo apenas `X-Request-ID` e `IS_IP`. Porém há **bugs nas chaves de remoção**:

```java
// Populado com:
MDC.put("Request-Content-Type", req.getContentType());  // chave = "Request-Content-Type"
MDC.put("Request-Body", body);                           // chave = "Request-Body"

// Removido com (ERRADO):
MDC.remove("Content-Type");   // ← BUG: chave não existe — "Request-Content-Type" vaza
MDC.remove("Body");           // ← BUG: chave não existe — "Request-Body" vaza
```

Estado real do MDC durante o processamento da requisição (após o log inicial):

| Campo MDC | Intenção | Real |
|---|---|---|
| `X-Request-ID` | Persiste ✓ | Persiste ✓ |
| `IS_IP` | Persiste ✓ | Persiste ✓ |
| `Request-Method` | Removido ✓ | Removido ✓ |
| `Request-URI` | Removido ✓ | Removido ✓ |
| `Headers` | Removido ✓ | Removido ✓ |
| `Request-Content-Type` | Removido ✗ | **Vaza** ⚠️ |
| `Request-Body` | Removido ✗ | **Vaza** ⚠️ |

#### Outros problemas identificados

**1. `Filter` em vez de `OncePerRequestFilter`**

Ambos os filtros implementam `jakarta.servlet.Filter` diretamente. Em cenários com `forward` ou `include` (ex: forward de erro do Spring), o filtro pode executar duas vezes na mesma requisição. `OncePerRequestFilter` previne isso com um flag por request.

**2. Sem `finally` para `MDC.clear()`**

`MDC.clear()` fica em `LoggingFinalFilter`. Se uma exceção escapar da cadeia de filtros inteira (antes de chegar ao `LoggingFinalFilter`), o MDC nunca é limpo — risco de vazamento entre threads do pool.

**3. Filtro condicional por URI**

Ambos os filtros só executam se `req.getRequestURI().contains("/api")`. URIs fora desse padrão (ex: `/actuator`, `/health`) nunca têm `X-Request-ID` no MDC — e `ExceptionsHandler` não conseguiria incluir no `ProblemDetail` para erros nessas rotas.

**4. `X-Request-ID` não é ecoado na response**

O header não é devolvido ao cliente. Impossível correlacionar um erro sem consultar logs do servidor.

**5. `ExceptionsHandler` não lê MDC**

`X-Request-ID` está disponível no MDC durante o processamento, mas o handler não o inclui na resposta de erro. Perde a correlação log ↔ erro.

**6. Sem propagação para threads assíncronas**

Serviços com `@Async` perdem contexto MDC — nenhum `TaskDecorator` configurado na foundation.

---

### Proposta: correções e ajustes no MDC existente

#### 1. Corrigir chaves erradas no `MDC.remove`

```java
// Substituir:
MDC.remove("Content-Type");
MDC.remove("Body");

// Por:
MDC.remove("Request-Content-Type");
MDC.remove("Request-Body");
```

#### 2. Migrar para `OncePerRequestFilter`

```java
// Antes:
public class LoggingInitialFilter implements Filter { ... }

// Depois:
public class LoggingInitialFilter extends OncePerRequestFilter { ... }
```

#### 3. Garantir `MDC.clear()` em `finally`

Mover `MDC.clear()` para um bloco `finally` no `LoggingInitialFilter` (que tem `@Order(0)` e envolve toda a cadeia):

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                 FilterChain chain) throws ServletException, IOException {
    try {
        // popular MDC, logar, chamar chain
        chain.doFilter(req, response);
    } finally {
        MDC.clear();  // garante limpeza mesmo em exceções
    }
}
```

Isso elimina a necessidade do `MDC.clear()` no `LoggingFinalFilter`.

#### 4. Separar MDC base (universal) do logging detalhado (condicional)

`X-Request-ID` deve ser populado para qualquer requisição, independente da URI:

```java
// Sempre (fora do if):
MDC.put("X-Request-ID", getXRequestId(req));
MDC.put("IS_IP", getClientIp(req));
response.setHeader("X-Request-ID", MDC.get("X-Request-ID"));  // eco ao cliente

// Condicional (dentro do if URI matches):
if (req.getRequestURI().contains(scosFilterProperties.getURI())) {
    // log detalhado de request/response
}
```

#### 5. Enriquecer `ProblemDetail` com `X-Request-ID` e `timestamp`

No `ExceptionsHandler`, após montar o `ProblemDetail`:

```java
private ProblemDetail enrich(ProblemDetail problem) {
    String requestId = MDC.get("X-Request-ID");
    if (requestId != null) {
        problem.setProperty("requestId", requestId);
    }
    problem.setProperty("timestamp", Instant.now().toString());
    return problem;
}
```

`timestamp` em ISO-8601 UTC é prática consolidada no mercado (Spring Boot Actuator, AWS, GCP usam esse formato). Permite rastreabilidade temporal sem consultar logs.

Resultado na response de erro:

```json
{
  "type": "https://docs.sawcunhaos.com.br/problems/business-error",
  "title": "Business Error",
  "status": 400,
  "detail": "CPF informado é inválido.",
  "instance": "/api/persons",
  "code": "SCOS-007",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-05-29T14:32:10.456Z"
}
```

#### 6. `TaskDecorator` para threads assíncronas

```java
executor.setTaskDecorator(runnable -> {
    Map<String, String> context = MDC.getCopyOfContextMap();
    return () -> {
        try {
            MDC.setContextMap(context != null ? context : Map.of());
            runnable.run();
        } finally {
            MDC.clear();
        }
    };
});
```

---

### Plano de testes para os filtros MDC

**Infraestrutura disponível:** JUnit 5 + Mockito + `spring-test` (já no `pom.xml` de `utils`). Todos os testes podem ser unitários com `MockHttpServletRequest`, `MockHttpServletResponse` e `MockFilterChain` — sem Spring context.

#### `LoggingInitialFilterTest`

| Cenário | Verificação |
|---|---|
| URI `/api/users` com header `X-Request-ID: abc` | `MDC.get("X-Request-ID") == "abc"` antes de `chain.doFilter` |
| URI `/api/users` sem header `X-Request-ID` | `MDC.get("X-Request-ID")` é UUID válido |
| URI `/actuator/health` | MDC não contém `X-Request-ID` **antes da correção**; após correção, contém |
| Após chain executar | `Request-Content-Type` e `Request-Body` ausentes do MDC (valida correção das chaves) |
| `X-Request-ID` ecoado na response | `response.getHeader("X-Request-ID")` não nulo |
| Exceção lançada na chain | MDC limpo após (valida `finally`) |

```java
@ExtendWith(MockitoExtension.class)
class LoggingInitialFilterTest {

    @Mock ScosFilterProperties properties;
    @Mock SanitizationHeadersComponent headersComponent;
    @Mock SanitizationBodyComponent bodyComponent;
    @Mock IpAddressExtractor ipExtractor;

    @InjectMocks LoggingInitialFilter filter;

    @Test
    void shouldUseProvidedXRequestIdHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("X-Request-ID", "my-fixed-id");
        when(properties.getURI()).thenReturn("/api");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        // após filter chain, MDC já foi limpo — validar DENTRO da chain via spy/captor
    }

    @Test
    void shouldGenerateXRequestIdWhenHeaderAbsent() throws Exception { ... }

    @Test
    void shouldNotLeakRequestContentTypeAfterInitialLog() throws Exception { ... }

    @Test
    void shouldClearMdcEvenWhenChainThrows() throws Exception { ... }
}
```

#### `LoggingFinalFilterTest`

| Cenário | Verificação |
|---|---|
| URI `/api/users`, status 200 | `MDC.get("Response-Code") == "200"` durante log |
| URI `/actuator` | MDC não populado, `MDC.clear()` não chamado |
| Após `doFilter` completar | MDC limpo |

#### `ExceptionsHandlerMdcTest`

| Cenário | Verificação |
|---|---|
| `ScosException` com `X-Request-ID` no MDC | `ProblemDetail.getProperties().get("requestId")` igual ao valor do MDC |
| `ScosException` sem `X-Request-ID` no MDC | `requestId` ausente do `ProblemDetail` (não quebra) |
| `MethodArgumentNotValidException` com MDC | `requestId` presente |

### Cadeia de filtros (após correções)

```
Request
  └─ LoggingInitialFilter (@Order 0)          ← X-Request-ID sempre; log detalhado condicional
       └─ ExceptionHandlerFilter (security)   ← captura ScosSecurityException
            └─ Spring Security filters
                 └─ Controller → @ControllerAdvice
                                    └─ ExceptionsHandler ← lê X-Request-ID do MDC → ProblemDetail
  [finally LoggingInitialFilter]              ← MDC.clear() garantido
```

---

## Validação: conformidade com convenções SCOS e mercado

### Conflitos com convenções SCOS atuais

Esta ideia propõe **alterar a foundation** — e portanto as próprias convenções. Os três conflitos abaixo não são bloqueadores, mas precisam ser reconhecidos e os artefatos de convenção devem ser atualizados junto com a implementação.

#### Conflito 1 — `rest-and-errors.md` proíbe `ProblemDetail` explicitamente

> "Do NOT write a `@RestControllerAdvice`/`ProblemDetail` handler. The foundation's `ExceptionsHandler` is already global and returns a localized `ExceptionResponse` (`message`, `codeError`, `validationErrors`)."

A ideia propõe exatamente o oposto: adotar `ProblemDetail` nativo e remover `ExceptionResponse`. Como a mudança é **na própria foundation**, a convenção precisa ser atualizada para refletir o novo padrão — não é uma violação de serviço, é uma evolução do contrato.

**Ação necessária:** atualizar `rest-and-errors.md` e `scos-conventions/SKILL.md` como parte do PR desta mudança.

#### Conflito 2 — `ScosResponseDTO` é o padrão de resposta

A convenção diz: "Return `ScosResponseDTO<T>` from endpoints." O `ExceptionsHandler` atual retorna `ScosResponseDTO<ExceptionResponse>`. A ideia remove o wrapper para erros.

**Resolução:** o `ScosResponseDTO` permanece para respostas de **sucesso** (`2xx`). Para erros (`4xx`/`5xx`), o `Content-Type: application/problem+json` é semanticamente incompatível com o wrapper. A convenção deve ser clarificada: `ScosResponseDTO` é para sucesso; `ProblemDetail` é para erros.

#### Conflito 3 — `ScosProblemType` não integra com `ExceptionCode`

A ideia propõe um enum `ScosProblemType` paralelo ao `ExceptionCode` existente. Isso cria dois sistemas de códigos de erro. A integração correta é enriquecer o contrato `ExceptionCode` com os metadados da RFC 9457:

```java
// Em vez de ScosProblemType separado:
public interface ExceptionCode {
    String getCode();
    
    // novos métodos default (opcionais — ExceptionCode existentes não quebram):
    default URI getType() {
        return URI.create("https://docs.sawcunhaos.com.br/problems/" + getCode().toLowerCase());
    }
    default String getTitle() { return "Error"; }
}
```

Assim `ScosExceptionCode.ATTRIBUTE_NOT_VALID` já carrega `type` e `title` sem enum extra.

---

### Problemas técnicos na proposta

#### Problema 1 — Subclasse de `ProblemDetail` tem riscos de serialização Jackson

A ideia propõe:

```java
public class ScosProblemDetail extends ProblemDetail { ... }
```

Spring 7's `ProblemDetail` usa `@JsonAnyGetter` para extensões. Subclasses com getters próprios (ex: `getCode()`) podem resultar em campos duplicados no JSON — um no objeto pai via `properties` e outro via getter do filho.

**Solução correta:** não subclassificar. Usar `ProblemDetail.forStatus()` + `setProperty()` para extensões:

```java
ProblemDetail problem = ProblemDetail.forStatus(status);
problem.setType(URI.create("..."));
problem.setTitle("Validation Error");
problem.setDetail(message);
problem.setProperty("code", "SCOS-001");
problem.setProperty("errors", validationErrors);
problem.setProperty("requestId", MDC.get("X-Request-ID"));
```

#### Problema 2 — `FieldError` conflita com `org.springframework.validation.FieldError`

A ideia usa:
```java
public record FieldError(String pointer, String detail) {}
```

`FieldError` já existe em `org.springframework.validation`. O nome causa conflito de import em qualquer classe que use ambos. Renomear para `ScosFieldError` ou `ProblemFieldError`.

#### Problema 3 — `@ResponseStatus` + retorno de `ProblemDetail`

Handlers atuais misturam `@ResponseStatus` (anotação) com retorno direto:

```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
protected ScosResponseDTO<ExceptionResponse> handleScosException(...) { ... }
```

Ao migrar para `ProblemDetail`, usar `ResponseEntity<ProblemDetail>` com status explícito no body — não `@ResponseStatus`. O status deve ser refletido no campo `status` do `ProblemDetail` E no HTTP status code da response.

#### Problema 4 — Versão do JUnit nos testes propostos

O `tdd-workflow` SKILL indica que o SCOS BOM traz **JUnit 6** (Jupiter API). Os exemplos de teste na seção MDC usam `@ExtendWith(MockitoExtension.class)` — isso é correto para JUnit 5/6. Confirmar que a versão no `pom.xml` do módulo `utils` está alinhada com o BOM antes de escrever os testes.

---

### O que está alinhado com mercado e SCOS

| Aspecto | Status |
|---|---|
| RFC 9457 como padrão de erro HTTP | ✅ Mercado consolidado (Spring 6+, Quarkus, Micronaut adotaram) |
| `ProblemDetail` nativo do Spring 7 | ✅ Idiomático para Spring Boot 4 |
| JSON Pointer para field errors | ✅ Alinhado com RFC 9457 §5.1 |
| `requestId` como campo de extensão | ✅ Prática comum (correlação log/erro) |
| `timestamp` ISO-8601 UTC como campo de extensão | ✅ Spring Boot Actuator, AWS, GCP usam o mesmo formato |
| URIs para `type` | ✅ RFC exige; URIs sem documentação inicial são aceitas |
| Preservar todos os dados atuais (`message`, `codeError`, `validationErrors`) | ✅ Nenhuma informação é descartada — apenas renomeada/reposicionada |
| Manter `ExceptionCode` + i18n via `LocaleService` | ✅ Preserva convenção SCOS de mensagens localizadas |
| Manter `ScosException` como mecanismo de throw | ✅ Sem breaking change na API dos serviços |
| Unificar 3 pontos de resposta de erro | ✅ Elimina inconsistência real |
| MDC já existente — apenas corrigir e enriquecer | ✅ Respeita infraestrutura existente |
| Testes com MockHttpServletRequest sem Spring context | ✅ Alinhado com TDD workflow SCOS |

### Resumo: o que a implementação deve fazer além do código

1. Atualizar `scos-conventions/SKILL.md` — seção de erros: substituir `ExceptionResponse` por `ProblemDetail`
2. Atualizar `spring-boot-service/references/rest-and-errors.md` — remover proibição de `ProblemDetail`
3. Enriquecer interface `ExceptionCode` com `getType()` e `getTitle()` defaults
4. Renomear `FieldError` para evitar conflito com Spring
5. Usar `setProperty()` em vez de subclassificar `ProblemDetail`
6. Documentar no CHANGELOG o mapeamento campo-a-campo: `data.message` → `detail`, `data.codeError` → `code`, `data.validationErrors[].attribute` → `errors[].pointer` (prefixar `#/`), `data.validationErrors[].message` → `errors[].detail`
7. Adicionar `timestamp` (ISO-8601 UTC) em todos os handlers via método `enrich()`

---

## Referências

- [RFC 9457 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
- [Spring Framework 7 — ProblemDetail](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-rest-exceptions)
- [RFC 6901 — JSON Pointer](https://www.rfc-editor.org/rfc/rfc6901) (para `pointer` nos erros de validação)
- [SLF4J MDC — Mapped Diagnostic Context](https://www.slf4j.org/manual.html#mdc)
- [Logback MDC — propagação e thread pools](https://logback.qos.ch/manual/mdc.html)
