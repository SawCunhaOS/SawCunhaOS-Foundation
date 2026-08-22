# Story 2.5: Corrigir a resolução de título de erro para Jackson 3

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor da API recebendo erros de deserialização,
Eu quero uma mensagem de erro correta mesmo com Jackson 3,
Para não receber um título genérico ou quebrado.

## Acceptance Criteria

1. **Given** um teste que confirma se o regex atual de `resolveTitle` ainda casa com as mensagens do Jackson 3, **When** o teste falha (regex desatualizado) ou passa, **Then** `resolveTitle` passa a navegar a causa (`InvalidFormatException`/`MismatchedInputException.getPath()`) em vez de usar regex.

## Tasks / Subtasks

- [ ] Task 0 — resolver a divergência de nome antes de tocar em código (bloqueante) (AC: #1)
  - [ ] **ATENÇÃO — discrepância confirmada entre o epics.md e o código real**: o epics.md descreve esta story como uma correção em um método chamado `resolveTitle` que usaria regex. No código atual, `ExceptionsHandler.resolveTitle` (linha 303-309) **não usa regex** — é só um `try/catch` em volta de `localeService.getMessage(title)` (esse é o alvo da **Story 2.6**, FR34, não desta). O regex baseado em `Pattern`/`Matcher` que de fato existe no arquivo está em `handleHttpMessageNotReadable` (linhas 85-98), tratando exceções de deserialização (`ex.getMessage()` de `HttpMessageNotReadableException`) — é esse método, não `resolveTitle`, que corresponde à intenção da AC (extrair campo/tipo de uma mensagem de erro de deserialização) e ao FR14 do PRD
  - [ ] Tratar esta story como sendo sobre `handleHttpMessageNotReadable`, não sobre o método literalmente chamado `resolveTitle` — se essa leitura estiver errada, escalar para quem mantém o épico antes de implementar, não decidir ad-hoc
- [ ] Task 1: Escrever o teste de regressão contra o regex atual (AC: #1)
  - [ ] Capturar uma mensagem real de `InvalidFormatException`/`MismatchedInputException` lançada pelo Jackson 3 (`tools.jackson.*`, confirmar a mensagem exata gerada por essa versão, não assumir o formato do Jackson 2) ao desserializar um enum inválido, e confirmar se os patterns atuais (`patternField = "(\\[\\\"[\\w,\\s]+\\\"\\])"`, `patternType = "(\\[[\\w,\\s]+\\])"`, linhas 86-87) ainda casam
- [ ] Task 2: Substituir regex por navegação de causa (AC: #1)
  - [ ] Reescrever `handleHttpMessageNotReadable` para extrair `field`/`typesEnum` navegando `ex.getCause()` quando for `InvalidFormatException`/`MismatchedInputException` (Jackson 3, pacote `tools.jackson.databind.exc`, não `com.fasterxml.jackson.databind.exc` — Jackson 2), usando `getPath()` (lista de `Reference`, cada uma com `getFieldName()`) e `getTargetType()`, em vez de fazer regex sobre `ex.getMessage()`
  - [ ] Manter o fallback atual (`field`/`typesEnum` vazios) para o caso em que a causa não é nenhum desses dois tipos, preservando o comportamento hoje existente para outras causas de `HttpMessageNotReadableException`

## Dev Notes

- **Isto não é uma correção pontual, é uma investigação de nomes primeiro**: o epics.md e o código real discordam sobre qual método faz o quê. Não implementar às cegas contra o nome do método — implementar contra o comportamento descrito (regex sobre mensagem de erro de deserialização Jackson).
- O projeto já usa Jackson 3 (`tools.jackson.*`, confirmado no `ScosJacksonConfig` e no ADD-6 do epics.md — `jackson-bom:3.2.1`), então os tipos de exceção corretos vêm de `tools.jackson.databind.exc.InvalidFormatException`/`MismatchedInputException`, não do pacote legado `com.fasterxml.jackson`.
- Esta story só toca `handleHttpMessageNotReadable`. `resolveTitle` (o método com esse nome literal) é tratado separadamente na Story 2.6 — não misturar as duas correções no mesmo commit.

### Project Structure Notes

- Arquivo modificado: `exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java` (método `handleHttpMessageNotReadable`, linhas 76-106).
- Novo teste em `exception/src/test/java/br/com/sawcunhaos/foundation/exception/`.

### References

- [Source: exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-25-corrigir-a-resolução-de-título-de-erro-para-jackson-3]
- [Source: _bmad-output/planning-artifacts/epics.md] (ADD-6 — versões fixadas, Jackson `jackson-bom:3.2.1`)

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
