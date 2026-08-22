# Story 2.6: Corrigir `resolveTitle` para não usar try/catch como controle de fluxo

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor da API localizada,
Eu quero receber o título de erro traduzido mesmo quando o `LocaleService` falha,
Para não ver um literal em inglês ("Business Error") numa API em PT-BR.

## Acceptance Criteria

1. **Given** um teste que reproduz o mascaramento de falha real do `LocaleService` pelo try/catch atual, **When** o teste é escrito e falha antes da correção, **Then** `LocaleService` ganha `getMessageOrDefault(code, default)`, com o default vindo de `ExceptionCode.getTitle()`.
2. **And** `resolveTitle` usa esse método em vez de try/catch.

## Tasks / Subtasks

- [ ] Task 1 — teste que reproduz o bug, TDD (NFR5) (AC: #1)
  - [ ] `ExceptionsHandler.resolveTitle` (linha 303-309): `try { return localeService.getMessage(title); } catch (Exception e) { return "Business Error"; }` — hoje **qualquer** exceção do `LocaleService.getMessage` (chave ausente, `MessageSource` mal configurado, erro de I/O no bundle) é silenciosamente engolida e substituída pelo literal fixo em inglês `"Business Error"`, mesmo que o motivo real seja um bug no `LocaleService` que deveria ser visível/logado
  - [ ] Escrever um teste que faz `localeService.getMessage(anyString())` lançar (mockado) e confirma que o resultado não é mais o literal hardcoded `"Business Error"`, mas sim o título vindo de `ExceptionCode.getTitle()` — deve falhar contra a implementação atual antes da correção
- [ ] Task 2: Adicionar `getMessageOrDefault` ao contrato `LocaleService` (AC: #1)
  - [ ] `utils/src/main/java/br/com/sawcunhaos/foundation/utils/specification/LocaleService.java` hoje só declara `getLocale()` e as duas sobrecargas de `getMessage(...)` — adicionar `String getMessageOrDefault(String code, String defaultValue)`
  - [ ] **Achado**: a única implementação de `LocaleService` em todo o repositório é `audit/src/test/java/br/com/sawcunhaos/foundation/audit/configuration/LocaleUtilsBean.java` — e ela é `src/test`, não produção. Não existe implementação de produção deste contrato neste repositório (é esperado que a aplicação consumidora forneça a sua). Atualizar `LocaleUtilsBean` mesmo assim, pois é a única referência usada nos testes do próprio foundation
  - [ ] Implementar `getMessageOrDefault` em `LocaleUtilsBean` usando a sobrecarga de 4 argumentos do Spring `MessageSource.getMessage(code, args, defaultValue, locale)` (que já aceita um default nativamente) — não usar try/catch, é exatamente o que o AC pede para eliminar
- [ ] Task 3: `resolveTitle` passa a usar o novo método (AC: #2)
  - [ ] Trocar o corpo de `resolveTitle` em `ExceptionsHandler.java` para `return localeService.getMessageOrDefault(title, /* ExceptionCode.getTitle() correspondente */);` — como `resolveTitle` hoje só recebe uma `String title` (não o `ExceptionCode` original), verificar se as duas chamadas existentes (`handleScosException`, linha 212, e `handleScosNoRollbackException`, linha 227) têm acesso ao `ExceptionCode`/`exception.getTitle()` do domínio para passar como default — `ScosException.title` (campo já existente, vindo de `code.getTitle()` no construtor) é exatamente esse valor, então o default é `exception.getTitle()`, não precisa buscar o `ExceptionCode` de novo

## Dev Notes

- **Bug real confirmado**: o `catch (Exception e) { return "Business Error"; }` mascara qualquer falha do `LocaleService`, não só "chave ausente" — inclusive bugs reais de configuração de i18n, que ficam invisíveis em produção porque o try/catch os transforma silenciosamente num literal fixo em inglês.
- Diferença chave desta story para a 2.5: aqui o alvo é literalmente o método `resolveTitle` (linha 303) — confirmado no código, sem ambiguidade de nome (ao contrário da Story 2.5, ver nota lá).
- `ScosException.title` já é populado a partir de `code.getTitle()` no construtor (`utils/.../exception/ScosException.java`) — é esse valor (não um novo lookup de `ExceptionCode`) que serve de default para `getMessageOrDefault`.
- Não introduzir um mecanismo genérico de "resolução de mensagem com fallback" reaproveitável em todo o projeto — o pedido é um método a mais na interface `LocaleService` já existente, não uma abstração nova.

### Project Structure Notes

- Arquivo modificado: `utils/src/main/java/br/com/sawcunhaos/foundation/utils/specification/LocaleService.java` (novo método de contrato).
- Arquivo modificado: `audit/src/test/java/br/com/sawcunhaos/foundation/audit/configuration/LocaleUtilsBean.java` (única implementação de `LocaleService` no repositório, escopo `test`).
- Arquivo modificado: `exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java` (método `resolveTitle`).
- Novo teste em `exception/src/test/java/br/com/sawcunhaos/foundation/exception/`.

### References

- [Source: exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java]
- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/specification/LocaleService.java]
- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/exception/ScosException.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-26-corrigir-resolvetitle-para-não-usar-trycatch-como-controle-de-fluxo]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
