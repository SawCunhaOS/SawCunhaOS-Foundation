# Story 2.3: Retornar 501 para funcionalidade não implementada

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor da API,
Eu quero que uma rota marcada como não implementada responda `501`,
Para distinguir isso de um erro genérico de servidor.

## Acceptance Criteria

1. **Given** uma chamada que lança `MethodNotImplementedException`, **When** o handler processa a exceção, **Then** a resposta é `501` em vez do `500` genérico atual.

## Tasks / Subtasks

- [ ] Task 1: Adicionar handler dedicado (AC: #1)
  - [ ] `exception/src/main/java/br/com/sawcunhaos/foundation/exception/error/MethodNotImplementedException.java` hoje **não** estende `ScosException` — é uma `RuntimeException` simples sem `code`/`httpCode`. Sem handler próprio, ela cai no `@ExceptionHandler(Exception.class)` genérico (`handleGenericException`, linha 270 de `ExceptionsHandler.java`) e responde `500`
  - [ ] Adicionar `@ExceptionHandler(MethodNotImplementedException.class)` em `ExceptionsHandler.java`, seguindo o mesmo padrão do handler de `AccessDeniedException` (que também não usa `ScosException`): montar o `ProblemDetail` via `ScosProblemDetails.of(...)` com `HttpStatus.NOT_IMPLEMENTED` (501) hardcoded, sem depender de um `getHttpCode()` de enum
  - [ ] Não existe hoje uma constante `ScosExceptionCode` para "não implementado" — adicionar um código de erro dedicado (ex.: `ScosExceptionCode.NOT_IMPLEMENTED("SCOS-010", "Not Implemented")`) em `utils/src/main/java/br/com/sawcunhaos/foundation/utils/enums/ScosExceptionCode.java`, seguindo exatamente o padrão dos 9 códigos já existentes ali (não reaproveitar `GENERIC`, que é semanticamente "erro interno genérico", não "não implementado")
- [ ] Task 2: Teste (AC: #1)
  - [ ] Teste unitário chamando o novo handler diretamente com uma instância de `MethodNotImplementedException`, mesmo padrão dos testes existentes (`@Mock LocaleService`, `@InjectMocks ExceptionsHandler`), confirmando `501` e o novo código/título

## Dev Notes

- **Confirmado no código atual**: `MethodNotImplementedException` (`exception/src/main/java/br/com/sawcunhaos/foundation/exception/error/MethodNotImplementedException.java`) estende `RuntimeException` puro — nenhum handler específico existe hoje em `ExceptionsHandler.java` para ela, então cai no fallback genérico e responde `500`.
- Não seguir o padrão de `ScosException`/`ScosNoRollbackException` (que recebem `ExceptionCode` no construtor) para `MethodNotImplementedException` — mudar a hierarquia dessa exceção está fora do escopo desta story (é só sobre a resposta HTTP do handler). Usar o mesmo padrão simples do handler de `AccessDeniedException`: montar o `ProblemDetail` direto no handler, sem exigir que a exceção carregue o `ExceptionCode`.
- Escopo intencionalmente pequeno: **não** adicionar aqui nenhuma lógica de "quais rotas retornam not-implemented" — a story só garante que, quando essa exceção específica é lançada (por quem já a usa hoje em código consumidor), a resposta HTTP é `501`.

### Project Structure Notes

- Arquivo modificado: `exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java` (novo `@ExceptionHandler`).
- Arquivo modificado: `utils/src/main/java/br/com/sawcunhaos/foundation/utils/enums/ScosExceptionCode.java` (novo enum constant `NOT_IMPLEMENTED`, código `SCOS-010`).
- Novo arquivo de teste em `exception/src/test/java/br/com/sawcunhaos/foundation/exception/`.

### References

- [Source: exception/src/main/java/br/com/sawcunhaos/foundation/exception/error/MethodNotImplementedException.java]
- [Source: exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java]
- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/enums/ScosExceptionCode.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-23-retornar-501-para-funcionalidade-não-implementada]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
