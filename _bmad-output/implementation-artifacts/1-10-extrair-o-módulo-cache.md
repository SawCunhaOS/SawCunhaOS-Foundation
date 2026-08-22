# Story 1.10: Extrair o módulo `cache`

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como time consumidor que usa cache Redis,
Eu quero um módulo `cache` isolado,
Para não herdar JPA ao habilitar apenas cache.

## Acceptance Criteria

1. **Given** `PolymorphicRedisSerializer` e demais classes de `configuration/cache/*` do inventário, **When** o módulo `cache` é extraído dependendo apenas de `core`, **Then** `cache` compila isoladamente e os testes migrados permanecem verdes.
2. **And** a extração ocorre em 2 commits separados.

## Tasks / Subtasks

- [ ] Task 1: Commit 1 — mover as classes de `configuration/cache/*` (AC: #1)
  - [ ] Criar módulo Maven `scos-foundation-cache`, pacote raiz `br.com.sawcunhaos.foundation.cache`, dependendo só de `core` + `spring-boot-starter-cache` + `spring-boot-starter-data-redis`
  - [ ] Mover: `utils/src/main/java/.../configuration/cache/PolymorphicRedisSerializer.java`, `ScosCacheConfiguration.java`, `ScosCacheKeyGenerator.java`, `configuration/cache/properties/ScosCacheModel.java`, `ScosCacheProperties.java`
  - [ ] `jackson-dataformat-smile` acompanha `PolymorphicRedisSerializer` para este módulo (é a única classe que a usa)
  - [ ] Mover testes correspondentes, se existentes
  - [ ] Commit isolado: só mover/renomear pacote, zero mudança de lógica
- [ ] Task 2: Commit 2 — ajustar o que precisar (AC: #2)
  - [ ] Aplicar separadamente qualquer ajuste de comportamento necessário; documentar se não houver nenhum
  - [ ] **Não antecipar** a correção de allowlist do `PolymorphicRedisSerializer` (`Class.forName` sobre valor vindo do Redis) nesta story — é a Story 3.16 (Épico 3, FR32) que faz essa correção de segurança; esta story só move o código como está hoje

## Dev Notes

- Depende da Story 1.7 (`core`) já concluída.
- **Achado de segurança relevante para quem for depois**: o plano de origem já sinaliza que `PolymorphicRedisSerializer` faz `Class.forName` sobre valor vindo do Redis sem allowlist — "é aqui que aterrissa a correção de allowlist apontada no review de segurança." Esta story move o código **sem corrigir** a vulnerabilidade (é escopo do FR32/Story 3.16, Épico 3) — não confundir "mover" com "corrigir": manter o comportamento idêntico é o próprio objetivo do AC #1/#2 (NFR4).
- Fase 4 do plano de origem: `cache` é o terceiro módulo folha a sair (depois de `spring`, `validation`).

### Project Structure Notes

- Módulo Maven novo: `cache/` — depende de `core`, `spring-boot-starter-cache`, `spring-boot-starter-data-redis`.
- `utils/` perde as classes de `configuration/cache/*`.
- `pom.xml` raiz ganha `<module>cache</module>`.

### References

- [Source: etc/doc/plano/plano-decomposicao-utils.md#scos-foundation-cache]
- [Source: _bmad-output/planning-artifacts/epics.md#story-110-extrair-o-módulo-cache]
- [Source: _bmad-output/planning-artifacts/epics.md#story-316-adicionar-allowlist-de-tipos-no-polymorphicredisserializer]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
