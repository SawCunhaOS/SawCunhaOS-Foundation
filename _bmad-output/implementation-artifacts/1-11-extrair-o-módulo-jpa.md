# Story 1.11: Extrair o módulo `jpa`

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como time consumidor que usa JPA,
Eu quero um módulo `jpa` isolado,
Para não herdar Redis/Feign ao habilitar apenas persistência.

## Acceptance Criteria

1. **Given** `SpecificationRepository`, `BaseEntity`, `JacksonCustomJsonFormatMapper`, `BaseLiquibaseProperties` do inventário, **When** o módulo `jpa` é extraído dependendo de `core` e `validation`, **Then** `jpa` compila isoladamente e os testes migrados permanecem verdes.
2. **And** a extração ocorre em 2 commits separados.

## Tasks / Subtasks

- [ ] Task 1: Commit 1 — mover as 4 classes (AC: #1)
  - [ ] Criar módulo Maven `scos-foundation-jpa`, pacote raiz `br.com.sawcunhaos.foundation.jpa`, dependendo de `core` (Story 1.7), `validation` (Story 1.9), `spring-boot-starter-data-jpa`, `querydsl-jpa`, `liquibase-core` (optional)
  - [ ] Mover: `utils/src/main/java/.../entity/BaseEntity.java`, `configuration/hibernate/JacksonCustomJsonFormatMapper.java`, `configuration/liquibase/BaseLiquibaseProperties.java`, `utils/SpecificationRepository.java`
  - [ ] `QBaseEntity.java` (`utils/target/generated-sources/`) é artefato gerado pelo `apt-maven-plugin`/QueryDSL — **não mover manualmente**, será regenerado no novo módulo pela mesma configuração de annotation processor (replicar o plugin `com.mysema.maven:apt-maven-plugin` do `utils/pom.xml` no `jpa/pom.xml`)
  - [ ] Mover testes correspondentes, se existentes
  - [ ] Commit isolado: só mover/renomear pacote
- [ ] Task 2: Commit 2 — ajustar o que precisar (AC: #2)
  - [ ] Aplicar separadamente qualquer ajuste de comportamento necessário; documentar se não houver nenhum

## Dev Notes

- Depende das Stories 1.7 (`core`) e 1.9 (`validation`) já concluídas — `jpa` é o primeiro módulo folha com dependência em outro módulo folha (`validation`), não só em `core`.
- Replicar a configuração do `apt-maven-plugin` (annotation processor QueryDSL) de `utils/pom.xml` para `jpa/pom.xml` — sem isso, `QBaseEntity` não é gerado e o build quebra silenciosamente em quem consome `Specification`/QueryDSL.
- Fase 4 do plano de origem: `jpa` é o quarto módulo folha a sair (depois de `spring`, `validation`, `cache`), antes de `web`.

### Project Structure Notes

- Módulo Maven novo: `jpa/` — depende de `core`, `validation`, `spring-boot-starter-data-jpa`, `querydsl-jpa`, `liquibase-core` (optional); replica o plugin `apt-maven-plugin` para geração de Q-classes.
- `utils/` perde `BaseEntity`, `JacksonCustomJsonFormatMapper`, `BaseLiquibaseProperties`, `SpecificationRepository`.
- `pom.xml` raiz ganha `<module>jpa</module>`.

### References

- [Source: etc/doc/plano/plano-decomposicao-utils.md#scos-foundation-jpa]
- [Source: utils/pom.xml#L233-L255] (config do `apt-maven-plugin` a replicar)
- [Source: _bmad-output/planning-artifacts/epics.md#story-111-extrair-o-módulo-jpa]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
