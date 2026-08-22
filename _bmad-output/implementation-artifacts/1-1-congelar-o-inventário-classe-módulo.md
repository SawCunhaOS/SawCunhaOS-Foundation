# Story 1.1: Congelar o inventário classe→módulo

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como mantenedor do `scos-foundation`,
Eu quero um inventário congelado de destino para cada uma das 71 classes de `utils`,
Para que nenhuma migração posterior deixe uma classe sem módulo de destino.

## Acceptance Criteria

1. **Given** as 71 classes atuais de `scos-foundation-utils`, **When** o inventário de destino é revisado, **Then** cada classe tem exatamente um módulo-alvo listado.
2. **And** nenhum destino é `utils.utils` ou pacote genérico equivalente.
3. **And** o inventário está documentado e acessível como referência única para as demais stories deste epic.

## Tasks / Subtasks

- [ ] Task 1: Confirmar a contagem real de classes em `utils/src/main/java` bate com 71 (AC: #1)
  - [ ] Rodar `find utils/src/main/java -name "*.java" | wc -l` e reconciliar qualquer divergência com a tabela da Seção 3 do plano de origem antes de congelar
- [ ] Task 2: Consolidar o inventário classe→módulo num artefato único e citável (AC: #1, #3)
  - [ ] Criar `_bmad-output/implementation-artifacts/inventario-classe-modulo.md` com uma tabela `classe completa (FQN atual) → módulo destino → pacote destino`, cobrindo as 71 classes sem omissão
  - [ ] Basear a tabela nas Seções 3 e "Todas as anotações, por destino" de `etc/doc/plano/plano-decomposicao-utils.md` (já congela `core`, `spring`, `web`, `cache`, `jpa`, `validation`, `audit-api`, `jdempotent-api`, `validation-api`) e na tabela de exemplos do addendum do PRD
  - [ ] Confirmar explicitamente os dois pontos que o plano original registra como lacuna resolvida na Fase 0: `annotation/rules/*` (`ScosRule`, `ScosRuleService`) → `spring`; `listener/ScosOnStartupListener` → `spring` (a interface `ScosStartupListener` fica em `core`)
- [ ] Task 3: Validar que nenhum destino usa nome genérico (AC: #2)
  - [ ] Checar que nenhuma linha da tabela aponta para um sub-pacote `util`/`common`/`utils.utils` dentro de `core` ou de qualquer módulo novo
- [ ] Task 4: Tornar o inventário referência única (AC: #3)
  - [ ] Adicionar ao topo do arquivo uma nota "fonte congelada — Fase 0, Story 1.1" e linkar de volta para o plano de origem e o addendum, para que Stories 1.5–1.13 apontem para este arquivo único em vez de recitarem a tabela

## Dev Notes

- Esta story **não move nenhum arquivo Java** — é só o congelamento documental do destino. Nenhum código muda.
- Fonte primária já existente com a decomposição completa das 71 classes: `etc/doc/plano/plano-decomposicao-utils.md` (Seção 3 "Módulos-alvo" e Seção "Todas as anotações, por destino"). Não redigitar do zero — extrair e formalizar como tabela única.
- Fonte secundária (exemplos, não exaustiva): `_bmad-output/planning-artifacts/prds/prd-SawCunhaOS-Foundation-2026-08-18/addendum.md`.
- Levantamento already-done da árvore atual (para conferência, não repetir a exploração): pacotes de `utils/src/main/java/br/com/sawcunhaos/foundation/utils/` — `adapter` (3), `annotation` (1 + `audit`:2, `jdempotent`:5, `normalizestrings`:1, `request`:6, `rules`:2), `aspect` (1), `configuration/cache` (5), `configuration/feign` (2), `configuration/hibernate` (1), `configuration/liquibase` (1), `configuration/rest` (1 + `filter`:4), `dto` (1+2), `entity` (1), `enums` (4), `exception` (1), `listener` (1), `sort` (1), `specification` (5), `utils` (9, inclui `GsonUtils` que será removido na Story 1.3, não migrado), `validation` (`taxIdentifier`:6, `zipcode`:2), `valueobjects` (4).
- **NFR4** (escopo de mudança): esta story é puramente documental — nenhuma alteração de comportamento, nenhum arquivo Java tocado.

### Project Structure Notes

- Novo artefato: `_bmad-output/implementation-artifacts/inventario-classe-modulo.md` (não é código de produção, é o registro de referência do épico).
- Nenhum módulo Maven é criado ou alterado nesta story.

### References

- [Source: etc/doc/plano/plano-decomposicao-utils.md#3-módulos-alvo]
- [Source: etc/doc/plano/plano-decomposicao-utils.md#todas-as-anotações-por-destino]
- [Source: _bmad-output/planning-artifacts/prds/prd-SawCunhaOS-Foundation-2026-08-18/addendum.md#mapeamento-classe--módulo-inventário-de-referência]
- [Source: _bmad-output/planning-artifacts/epics.md#story-11-congelar-o-inventário-classemódulo]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Foundation-2026-08-19/ARCHITECTURE-SPINE.md#ad-1--fronteira-e-direção-de-dependência-entre-módulos-adopted]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
