# Story 2.1: Corrigir o handler de acesso negado (403)

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor da API protegida por Spring Security,
Eu quero que uma negação de autorização real responda `403`,
Para não receber `500` num erro que na verdade é de permissão.

## Acceptance Criteria

1. **Given** um teste que reproduz o bug (chamada negada por `org.springframework.security.access.AccessDeniedException` hoje capturada como `500`), **When** o teste é escrito e falha antes da correção, **Then** o handler passa a capturar `AccessDeniedException` e responde `403`.
2. **And** o handler existente para `AuthorizationDeniedException` continua lado a lado, sem substituição.

## Tasks / Subtasks

- [ ] Task 1 — teste que reproduz o bug, TDD (NFR5) (AC: #1)
  - [ ] Escrever um teste que invoca `ExceptionsHandler.handleAccessDeniedException` com uma instância de `org.springframework.security.access.AccessDeniedException` e espera `403`. Antes da correção este teste FALHA — hoje esse tipo não é capturado por nenhum `@ExceptionHandler` específico e cai no `@ExceptionHandler(Exception.class)` genérico (`handleGenericException`, linha 270 de `ExceptionsHandler.java`), que responde `500`
  - [ ] Confirmar a falha rodando o teste antes de tocar no código de produção
- [ ] Task 2: Trocar o tipo capturado pelo handler (AC: #1)
  - [ ] Em `exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java`, trocar o import `java.nio.file.AccessDeniedException` (linha 45) por `org.springframework.security.access.AccessDeniedException`
  - [ ] O método `handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request)` (linha 240-252) já responde `403` corretamente — a correção é só o tipo capturado, a lógica interna não muda
- [ ] Task 3: Corrigir o teste existente que hoje usa o tipo errado (AC: #1)
  - [ ] `exception/src/test/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandlerAccessDeniedTest.java` hoje importa e instancia `java.nio.file.AccessDeniedException` — trocar para `org.springframework.security.access.AccessDeniedException` (o construtor de ambos aceita uma `String`, a chamada `new AccessDeniedException("/api/admin/users")` continua válida sem outra alteração)
- [ ] Task 4: Confirmar que os dois handlers continuam lado a lado (AC: #2)
  - [ ] Confirmar que `handleAccessDeniedException(AuthorizationDeniedException ex, HttpServletRequest request)` (linha 254-266) não é tocado nesta story — nenhuma alteração de assinatura, nenhuma remoção

## Dev Notes

- **Bug real confirmado no código atual** (`exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java`): existem hoje 2 handlers de "acesso negado" — um para `java.nio.file.AccessDeniedException` (linha 240, **tipo errado**, nunca lançado pelo Spring Security em runtime) e um para `org.springframework.security.access.AuthorizationDeniedException` (linha 254, correto e já funcional). Quando o Spring Security lança sua própria `org.springframework.security.access.AccessDeniedException` (que é o tipo real usado por `@PreAuthorize`/voters mais antigos, distinto de `AuthorizationDeniedException` que é o mecanismo mais novo), nenhum handler específico casa — cai no `Exception.class` genérico e responde `500`. Esta story fecha exatamente essa lacuna.
- O teste existente `ExceptionsHandlerAccessDeniedTest.java` hoje passa com o tipo nio porque o handler (mesmo com o tipo errado) responde 403 para o que foi instanciado nele — o teste não expõe o bug porque testa o handler com o tipo que ele já captura, não o tipo real que o Spring Security lança. Por isso a Task 1 exige escrever contra o tipo `org.springframework.security.access.AccessDeniedException` especificamente.
- **NFR5** (TDD): o teste da Task 1 deve ser escrito e confirmado falhando antes de tocar em `ExceptionsHandler.java`.
- Não modificar `ScosProblemDetails`, `LocaleService` nem o `ScosExceptionCode.ACCESS_DENIED` — o formato de resposta (403, título "Access Denied", `SCOS-004`) já está correto, só o tipo capturado está errado.

### Project Structure Notes

- Arquivo modificado: `exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java` (só o import, linha 45).
- Arquivo modificado: `exception/src/test/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandlerAccessDeniedTest.java` (só o import/instanciação).
- Nenhum módulo novo, nenhuma dependência nova.

### References

- [Source: exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java]
- [Source: exception/src/test/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandlerAccessDeniedTest.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-21-corrigir-o-handler-de-acesso-negado-403]
- [Source: _bmad-output/planning-artifacts/prds/prd-SawCunhaOS-Foundation-2026-08-18/prd.md]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
