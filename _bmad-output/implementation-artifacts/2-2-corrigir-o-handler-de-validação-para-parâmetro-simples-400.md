# Story 2.2: Corrigir o handler de validação para parâmetro simples (400)

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor da API,
Eu quero que uma violação em `@RequestParam @Min(1) int page` responda `400`,
Para não receber `500` por uma exceção não tratada.

## Acceptance Criteria

1. **Given** um teste que reproduz o `IndexOutOfBoundsException` hoje lançado em `getBeanResults().get(0)` para parâmetro simples, **When** o teste é escrito e falha antes da correção, **Then** o handler passa a iterar sobre `getBeanResults()` e `getValueResults()` e responde `400`.

## Tasks / Subtasks

- [ ] Task 1 — teste que reproduz o bug, TDD (NFR5) (AC: #1)
  - [ ] Escrever um teste para `ExceptionsHandler.handleHandlerMethodValidationException` (linha 140-168 de `ExceptionsHandler.java`) que simula um `HandlerMethodValidationException` cujo `getBeanResults()` retorna lista vazia (caso real de violação em parâmetro simples anotado direto, ex.: `@RequestParam @Min(1) int page`) — hoje `ex.getBeanResults().get(0)` (linha 149) lança `IndexOutOfBoundsException` nesse cenário, que escapa do `@ExceptionHandler` e vira `500` não tratado
  - [ ] Confirmar a falha do teste antes de tocar no código de produção
- [ ] Task 2: Iterar sobre `getBeanResults()` e `getValueResults()` (AC: #1)
  - [ ] Em `handleHandlerMethodValidationException`, substituir `ex.getBeanResults().get(0).getFieldErrors().forEach(...)` (linha 149) por iteração segura sobre `ex.getBeanResults()` (violações em objetos `@Valid`, mapeadas para `ScosFieldError` via `getFieldErrors()`, como já ocorre) **e** sobre `ex.getValueResults()` (violações em parâmetro simples anotado direto — cada `ParameterValidationResult` expõe o parâmetro e a lista de `MessageSourceResolvable` das violações, sem `getFieldErrors()`)
  - [ ] Reaproveitar `ScosFieldError.of(...)` e `localeService.getMessage(...)` já usados no restante do método — mesma forma de erro (`ScosProblemDetails.ofValidation`), sem criar um segundo formato para parâmetro simples

## Dev Notes

- **Bug real confirmado**: `exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java`, método `handleHandlerMethodValidationException` (linha 140-168), linha 149: `ex.getBeanResults().get(0).getFieldErrors().forEach(...)`. `HandlerMethodValidationException.getBeanResults()` só é não-vazio quando a violação ocorre em um parâmetro anotado `@Valid` (um bean); para um parâmetro simples anotado diretamente (`@RequestParam @Min(1) int page`), a violação aparece em `getValueResults()`, e `getBeanResults()` retorna lista vazia — `.get(0)` lança `IndexOutOfBoundsException`.
- `ParameterValidationResult` (o tipo de item de `getValueResults()`) não tem `getFieldErrors()` — expõe `getMethodParameter()` e `getResolvableErrors()` (lista de `MessageSourceResolvable`). O código precisa tratar os dois tipos de resultado com formas de extração diferentes, unificando no mesmo `List<ScosFieldError>` que já alimenta `ScosProblemDetails.ofValidation`.
- Reaproveitar `getArgsValidation` de `ExceptionUtils` (já usado nas outras 3 chamadas do mesmo padrão neste arquivo) para extrair os argumentos de mensagem de cada `MessageSourceResolvable`, mantendo consistência com `handleMethodArgumentNotValid` e `handleConstraintViolationException`.
- **NFR5** (TDD): o teste da Task 1 deve reproduzir o `IndexOutOfBoundsException` e falhar antes da correção.
- Este handler já está registrado corretamente (`@Override protected ResponseEntity<Object> handleHandlerMethodValidationException(...)`, sobrescreve o método do `ResponseEntityExceptionHandler`) — a correção é só na lógica interna, não no registro.

### Project Structure Notes

- Arquivo modificado: `exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java` (método `handleHandlerMethodValidationException`).
- Novo arquivo de teste (ou extensão de `ExceptionsHandlerValidationTest.java` existente, se o cenário de parâmetro simples couber ali sem misturar responsabilidades — decisão do dev-agent ao ver o arquivo).

### References

- [Source: exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java]
- [Source: exception/src/test/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandlerValidationTest.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-22-corrigir-o-handler-de-validação-para-parâmetro-simples-400]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
