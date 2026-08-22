# Story 1.8: Extrair o módulo `spring`

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como time consumidor que usa aspectos genéricos do Spring,
Eu quero um módulo `spring` isolado,
Para não herdar JPA/Servlet ao usar `ScosRule`.

## Acceptance Criteria

1. **Given** as classes do inventário destinadas a `spring` (`ScosRule`, `ScosRuleService`, `NormalizeStrings`, `StringProcessingAspect`, `ScosOnStartupListener`, `ScosStartupListener`), **When** o módulo é extraído em 2 commits separados, **Then** `spring` depende de `core` mas não importa `jakarta.servlet`, `jakarta.persistence` nem `spring.data`.
2. **And** uma regra ArchUnit local em `spring` impõe essa restrição.
3. **And** os testes migrados permanecem verdes.

## Tasks / Subtasks

- [ ] Task 1: Commit 1 — mover as 6 classes (AC: #1)
  - [ ] Criar módulo Maven `scos-foundation-spring`, pacote raiz `br.com.sawcunhaos.foundation.spring`, dependendo de `core` (Story 1.7) + `spring-context` + `spring-aop`
  - [ ] Mover: `utils/src/main/java/.../annotation/rules/ScosRule.java`, `ScosRuleService.java`, `annotation/normalizestrings/NormalizeStrings.java`, `aspect/StringProcessingAspect.java`, `listener/ScosOnStartupListener.java`
  - [ ] Mover `specification/ScosStartupListener.java` **para cá**, não para `core` — apesar do addendum do PRD listá-la como `core`, seu método `onStartupSystem(ApplicationReadyEvent event)` importa `org.springframework.boot.context.event.ApplicationReadyEvent`, violando a regra de zero-Spring do `core` (confirmado pela própria Story 1.7, AC #4) — migra junto de `ScosOnStartupListener`, que já a consome via `List<ScosStartupListener>`
  - [ ] Commit isolado: só mover/renomear pacote, zero mudança de lógica
- [ ] Task 2: Commit 2 — ajustar o que precisar (AC: #1)
  - [ ] Aplicar separadamente qualquer ajuste de comportamento necessário; documentar se não houver nenhum
- [ ] Task 3: Regra ArchUnit local (AC: #2)
  - [ ] Teste ArchUnit dentro do módulo `spring` que falha se qualquer classe importar `jakarta.servlet..`, `jakarta.persistence..` ou `org.springframework.data..`
- [ ] Task 4: Confirmar testes verdes (AC: #3)
  - [ ] Rodar a suíte migrada; nenhum teste específico destas 6 classes foi localizado em `utils/src/test` nesta análise — se a migração revelar ausência de cobertura, registrar nas Completion Notes em vez de inventar teste fora do escopo desta story de extração

## Dev Notes

- Depende da Story 1.7 (`core`) já concluída — `spring` importa `core`.
- Propósito do módulo, citado no plano de origem: "existe para que o `core` continue livre de Spring — sem ele, essas classes forçariam a quebra dessa regra."
- `ScosRule`/`ScosRuleService` não têm nenhum consumidor dentro da própria foundation (são estereótipos para as aplicações consumidoras) — isso é esperado, não é sinal de código morto a remover nesta story.
- Fase 4 do plano de origem: ordem de extração dos módulos folha é `spring` → `validation` → `cache` → `jpa` → `web` (do menos para o mais acoplado) — `spring` é o primeiro depois de `core`.

### Project Structure Notes

- Módulo Maven novo: `spring/` — depende de `core`, `spring-context`, `spring-aop`.
- `utils/` perde as 6 classes migradas (incluindo `ScosStartupListener`, cujo destino final diverge do addendum do PRD conforme já sinalizado na Story 1.7).
- `pom.xml` raiz ganha `<module>spring</module>`.

### References

- [Source: etc/doc/plano/plano-decomposicao-utils.md#scos-foundation-spring]
- [Source: _bmad-output/implementation-artifacts/1-7-extrair-o-módulo-core.md]
- [Source: _bmad-output/planning-artifacts/epics.md#story-18-extrair-o-módulo-spring]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
