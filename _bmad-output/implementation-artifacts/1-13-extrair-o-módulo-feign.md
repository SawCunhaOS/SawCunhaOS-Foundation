# Story 1.13: Extrair o módulo `feign`

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como time consumidor que integra via Feign,
Eu quero um módulo `feign` dedicado,
Para não herdar essa dependência ao usar só `web` ou `core`.

## Acceptance Criteria

1. **Given** `JacksonEncoderCustom`, `JacksonDecoderCustom` do inventário, **When** o módulo `feign` é extraído dependendo apenas de `core`, **Then** `feign` compila isoladamente e os testes migrados permanecem verdes.
2. **And** a extração ocorre em 2 commits separados.

## Tasks / Subtasks

- [ ] Task 1: Commit 1 — mover as 2 classes (AC: #1)
  - [ ] Criar módulo Maven `scos-foundation-feign`, pacote raiz `br.com.sawcunhaos.foundation.feign`, dependendo só de `core` (Story 1.7) + `io.github.openfeign:feign-core`
  - [ ] Mover: `utils/src/main/java/.../configuration/feign/JacksonEncoderCustom.java`, `JacksonDecoderCustom.java`
  - [ ] Mover testes correspondentes, se existentes
  - [ ] Commit isolado: só mover/renomear pacote
- [ ] Task 2: Commit 2 — ajustar o que precisar (AC: #2)
  - [ ] Aplicar separadamente qualquer ajuste de comportamento necessário; documentar se não houver nenhum

## Dev Notes

- Depende só da Story 1.7 (`core`) — módulo mais simples entre os folhas, 2 classes.
- **Decisão D3 do plano de origem já resolvida pelo AC**: módulo `feign` próprio, não dependência `optional` dentro de `web` com `@ConditionalOnClass` — "não vale um artefato para dois encoders" foi a inclinação do plano, mas o AC desta story (e a arquitetura, que lista `feign` como módulo folha independente dependendo só de `core`) fixa a opção do módulo dedicado como decisão final.
- Independente da ordem de `web` (Story 1.12) — pode rodar em paralelo ou antes/depois, já que não depende de `web` nem `cache`.

### Project Structure Notes

- Módulo Maven novo: `feign/` — depende só de `core` e `feign-core`.
- `utils/` perde `JacksonEncoderCustom.java`, `JacksonDecoderCustom.java`.
- `pom.xml` raiz ganha `<module>feign</module>`.

### References

- [Source: etc/doc/plano/plano-decomposicao-utils.md#scos-foundation-feign]
- [Source: etc/doc/plano/plano-decomposicao-utils.md#7-decisões-que-preciso-que-você-tome] (Decisão D3)
- [Source: _bmad-output/planning-artifacts/epics.md#story-113-extrair-o-módulo-feign]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
