# Story 2.10: Consolidar a constante `MDC_REQUEST_ID` no `core`

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como mantenedor do `scos-foundation`,
Eu quero uma única fonte para a constante de correlação de log,
Para eliminar duplicidade entre `web` e `core`.

## Acceptance Criteria

1. **Given** `MDC_REQUEST_ID` hoje duplicada entre `ScosProblemDetails` (agora em `web`, Story 2.9) e `LoggingInitialFilter` (em `web`, Epic 1 Story 1.12), **When** a constante é consolidada em um único ponto no módulo `core`, **Then** ambas as classes em `web` referenciam a mesma constante do `core`, sem duplicação.

## Tasks / Subtasks

- [ ] Task 1: Confirmar as duas definições atuais antes de consolidar (AC: #1)
  - [ ] `ScosProblemDetails.java` (movida para `web` na Story 2.9): `public static final String MDC_REQUEST_ID = "X-Request-ID";`
  - [ ] `LoggingInitialFilter.java` (movida para `web` na Story 1.12 do Epic 1): `public static final String REQUEST_ID_HEADER = "X-Request-ID";` — **nomes diferentes** (`MDC_REQUEST_ID` vs. `REQUEST_ID_HEADER`), mesmo valor literal (`"X-Request-ID"`). O AC descreve isso como a mesma constante duplicada — tratar como tal (mesmo propósito: chave de MDC/header de correlação), não como duas constantes distintas por coincidência
- [ ] Task 2: Criar a constante única em `core` (AC: #1)
  - [ ] Adicionar `MDC_REQUEST_ID` (manter esse nome — é o mais descritivo do propósito real, uso como chave de MDC) numa classe utilitária existente do `core` (ex.: `StringFieldUtils`, se fizer sentido semanticamente, ou uma constante estática simples — não criar uma classe nova só para uma constante, isso seria uma abstração para um valor que não muda)
  - [ ] Preferir uma constante simples num local já existente do `core` a criar um novo tipo/arquivo — o pedido é eliminar duplicação, não introduzir uma nova unidade de organização
- [ ] Task 3: Repontar as duas classes em `web` (AC: #1)
  - [ ] `ScosProblemDetails.enrich(...)` passa a usar a constante do `core`, removendo sua própria declaração local
  - [ ] `LoggingInitialFilter` passa a usar a mesma constante do `core` no lugar de `REQUEST_ID_HEADER` — confirmar que `LoggingFinalFilter` (que também referencia `X-Request-ID`, conforme Dev Notes da Story 1.12) segue a mesma mudança
  - [ ] Confirmar que nenhum teste existente (`LoggingFilterMaskingE2ETest`, `LoggingFinalFilterTest`, `LoggingInitialFilterTest`, `ExceptionsHandlerMdcTest`) depende do nome antigo da constante (`REQUEST_ID_HEADER`) via referência direta — se depender, atualizar a referência, não o valor

## Dev Notes

- Depende das Stories 2.9 (`ScosProblemDetails` já em `web`) e 1.12 do Epic 1 (`LoggingInitialFilter`/`LoggingFinalFilter` já em `web`) — antes disso as duas classes estão em módulos diferentes (`exception` e `utils`) e "consolidar em `core`" não teria as duas pontas já no lugar certo para repontar.
- Escopo mínimo: só a constante muda de dono, não o comportamento de MDC/logging em si — nenhuma outra alteração nos filtros ou no handler.
- Não introduzir uma classe `Constants`/`WebConstants` genérica no `core` para hospedar isso "para o futuro" — colocar a constante no local mais próximo do que já existe e faz sentido, seguindo a mesma regra ADD-5 (nada de sub-pacote genérico `util`/`common`).

### Project Structure Notes

- Módulo `core` ganha a constante `MDC_REQUEST_ID` (localização exata a decidir no código, dentro de uma classe já existente do `core`, sem criar arquivo novo só para isso).
- `web/.../model/ScosProblemDetails.java` e `web/.../configuration/rest/filter/LoggingInitialFilter.java` (e `LoggingFinalFilter.java`, se aplicável) removem sua declaração local e passam a importar a constante do `core`.

### References

- [Source: exception/src/main/java/br/com/sawcunhaos/foundation/exception/model/ScosProblemDetails.java]
- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/configuration/rest/filter/LoggingInitialFilter.java]
- [Source: _bmad-output/implementation-artifacts/1-12-extrair-o-módulo-web.md]
- [Source: _bmad-output/implementation-artifacts/2-9-mover-a-tradução-http-do-exception-para-o-web-e-registrar-vi.md]
- [Source: _bmad-output/planning-artifacts/epics.md#story-210-consolidar-a-constante-mdc_request_id-no-core]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
