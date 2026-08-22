# Story 1.9: Extrair o módulo `validation`

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como time consumidor que usa value objects brasileiros (CPF/CNPJ),
Eu quero um módulo `validation` próprio,
Para não carregar JPA completo só para validar um documento.

## Acceptance Criteria

1. **Given** `Cpf`, `Cnpj`, `Email`, `TaxIdentifier` e o restante do inventário destinado a `validation`, **When** o módulo é extraído dependendo de `core` e `validation-api`, com `jakarta.persistence-api` como `provided`, **Then** `validation` compila e os testes dos value objects permanecem verdes.
2. **And** a extração ocorre em 2 commits separados (mover vs. ajustar comportamento).

## Tasks / Subtasks

- [ ] Task 1: Commit 1 — mover value objects e validators (AC: #1)
  - [ ] Criar módulo Maven `scos-foundation-validation`, pacote raiz `br.com.sawcunhaos.foundation.validation`, dependendo de `core` (Story 1.7), `validation-api` (Story 1.5), `caelum-stella-core` e `jakarta.persistence-api` como `provided`
  - [ ] Mover os value objects: `utils/src/main/java/.../valueobjects/Cpf.java`, `Cnpj.java`, `Email.java`, `TaxIdentifier.java`
  - [ ] Mover os `ConstraintValidator`: `utils/src/main/java/.../validation/taxIdentifier/constraint/CnpjValidator.java`, `CpfValidator.java`, `TaxIdentifierValidator.java`, `validation/zipcode/constraint/ZipCodeValidator.java` — as anotações `@CPF`/`@CNPJ`/`@TaxIdentifier`/`@ZipCode` já foram para `validation-api` na Story 1.5; aqui só a lógica de validação
  - [ ] Mover as classes de anotação legadas `validation/taxIdentifier/CNPJ.java` e `CPF.java` **somente se ainda não migradas** para `validation-api` na Story 1.5 — conferir contra o inventário congelado da Story 1.1 para não duplicar
  - [ ] Mover os testes: `valueobjects/CnpjTest.java`, `CpfTest.java`, `EmailTest.java`, `TaxIdentifierTest.java`, `validation/zipcode/constraint/ZipCodeValidatorTest.java` (e o DTO de apoio `ZipCodeDTO.java`)
  - [ ] `jakarta.persistence-api` como `provided`: os value objects usam `@Embeddable` — só a anotação em tempo de compilação, sem arrastar runtime de persistência
  - [ ] Commit isolado: só mover/renomear pacote
- [ ] Task 2: Commit 2 — ajustar o que precisar (AC: #2)
  - [ ] Aplicar separadamente qualquer ajuste de comportamento necessário; documentar se não houver nenhum

## Dev Notes

- Depende das Stories 1.5 (`validation-api` com as anotações) e 1.7 (`core`) já concluídas.
- **Decisão D2 do plano de origem, já resolvida pelo AC**: `valueobjects` usam `@Embeddable`; ficam em `validation` (não em `jpa`) com `jakarta.persistence-api` como `provided` — "funciona, porque são só anotações, mas mistura conceitos" é o trade-off já aceito, não uma pergunta em aberto para o dev.
- Esta story tem interseção direta com a Story 1.3 (migração Gson→Jackson): a AC #6 daquela story pede para levantar se `Cpf`/`Cnpj`/`Email`/`TaxIdentifier` têm `TypeAdapter` Gson próprio **antes** de migrarem para cá — confirmar que a Story 1.3 já rodou e que esses value objects já serializam via Jackson antes de mover o pacote (evita mover um problema de serialização não resolvido).
- Fase 4 do plano de origem: `validation` é o segundo módulo folha a sair, depois de `spring`.

### Project Structure Notes

- Módulo Maven novo: `validation/` — depende de `core`, `validation-api`, `caelum-stella-core`, `jakarta.persistence-api` (`provided`).
- `utils/` perde os 4 value objects, os `ConstraintValidator` e os testes correspondentes.
- `pom.xml` raiz ganha `<module>validation</module>`.

### References

- [Source: etc/doc/plano/plano-decomposicao-utils.md#scos-foundation-validation]
- [Source: etc/doc/plano/plano-decomposicao-utils.md#7-decisões-que-preciso-que-você-tome] (Decisão D2)
- [Source: _bmad-output/planning-artifacts/epics.md#story-19-extrair-o-módulo-validation]
- [Source: _bmad-output/planning-artifacts/epics.md#story-13-migrar-serialização-json-de-gson-para-jackson]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
