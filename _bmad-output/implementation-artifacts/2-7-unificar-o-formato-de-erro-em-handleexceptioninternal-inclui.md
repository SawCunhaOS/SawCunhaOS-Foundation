# Story 2.7: Unificar o formato de erro em `handleExceptionInternal`, incluindo 404 nativo

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor da API,
Eu quero receber o mesmo formato de erro (`ScosProblemDetails`) em qualquer situação, inclusive rota inexistente,
Para não precisar tratar dois formatos diferentes.

## Acceptance Criteria

1. **Given** um teste que reproduz o formato nativo do Spring hoje devolvido para 404/método não suportado/media type inválido, **When** o teste é escrito e falha antes da correção, **Then** `handleExceptionInternal` é sobrescrito para unificar o formato em todos os casos, incluindo 404 de rota inexistente.

## Tasks / Subtasks

- [ ] Task 1 — pré-requisito bloqueante, confirmar antes de implementar (AC: #1)
  - [ ] **Achado crítico**: hoje não existe, em nenhum lugar do repositório, a propriedade `spring.mvc.throw-exception-if-no-handler-found=true` (confirmado por busca — nenhum `application.yml`/`.properties` do projeto define isso). Sem essa propriedade `true`, o Spring Boot **nunca lança** `NoResourceFoundException`/`NoHandlerFoundException` para rota inexistente — o `DispatcherServlet` despacha para o handler estático padrão, que produz a página de erro Whitelabel/JSON default do Boot **sem passar pelo `ControllerAdvice`**. Sobrescrever só `handleExceptionInternal` não é suficiente para cobrir 404 de rota inexistente se essa propriedade não estiver ligada na aplicação consumidora
  - [ ] Documentar essa propriedade como pré-requisito de configuração da aplicação consumidora (README do módulo `exception`/futuro `web`) — este módulo não pode setá-la sozinho por código (é uma propriedade do `DispatcherServlet` da aplicação hospedeira), mas deve alertar claramente que sem ela o 404 de rota inexistente não passa a ser unificado
- [ ] Task 2 — teste que reproduz o formato nativo, TDD (NFR5) (AC: #1)
  - [ ] Escrever um teste de contexto Spring MVC (`MockMvc`, com `throw-exception-if-no-handler-found=true` configurado no teste) que confirma que hoje 404/`HttpRequestMethodNotSupportedException`/`HttpMediaTypeNotAcceptableException` retornam o corpo nativo do Spring, não um `ScosProblemDetails` — deve falhar contra o comportamento atual
- [ ] Task 3: Sobrescrever `handleExceptionInternal` (AC: #1)
  - [ ] Em `ExceptionsHandler.java`, adicionar `@Override protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request)` — hoje esse método **não existe** na classe (herda o comportamento default de `ResponseEntityExceptionHandler`, que é o que produz o formato nativo para os casos não cobertos pelos `@Override` específicos já existentes: `NoResourceFoundException`, `HttpRequestMethodNotSupportedException`, `HttpMediaTypeNotAcceptableException`, etc.)
  - [ ] Reaproveitar `ScosProblemDetails.of(...)`/`enrich(...)` já usados nos outros handlers — o corpo unificado precisa ter o mesmo formato `type/title/status/detail/instance/code/requestId/timestamp`

## Dev Notes

- Esta é a única story do Epic 2 onde o gap real não está só no código do handler, mas numa propriedade de configuração da aplicação hospedeira — não é um detalhe menor, é o que decide se a AC #1 (404 de rota inexistente) é sequer alcançável sem mudança na aplicação consumidora. Sinalizar isso com destaque no PR/README, não deixar implícito.
- `handleExceptionInternal` é o método "coringa" do `ResponseEntityExceptionHandler` — todo `@Override` mais específico já existente (`handleHttpMessageNotReadable`, `handleMethodArgumentNotValid`, `handleHandlerMethodValidationException`) continua funcionando como está; esta story só cobre os casos que **não** têm um `@Override` específico e hoje caem no comportamento default do Spring.
- Não duplicar lógica: se o body/status já vier populado pelo Spring com informação útil, reaproveitar o que der (ex.: o `statusCode` recebido), só trocando o formato de saída para `ScosProblemDetails`.

### Project Structure Notes

- Arquivo modificado: `exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java` (novo `@Override handleExceptionInternal`).
- Novo teste de contexto MVC (`@WebMvcTest`/`MockMvc`) em `exception/src/test/java/br/com/sawcunhaos/foundation/exception/`.
- Documentação nova/atualizada: alertar sobre `spring.mvc.throw-exception-if-no-handler-found=true` como pré-requisito (README do módulo, ou nota na Story 2.9 quando o módulo virar autoconfiguração).

### References

- [Source: exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-27-unificar-o-formato-de-erro-em-handleexceptioninternal-incluindo-404-nativo]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
