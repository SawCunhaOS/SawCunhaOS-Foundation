# Story 1.2: Remover dependências mortas e obsoletas

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como mantenedor do `scos-foundation`,
Eu quero remover bibliotecas sem uso real e substituir as com uso mínimo,
Para reduzir a superfície de dependências transitivas antes de mover qualquer classe.

## Acceptance Criteria

1. **Given** o POM atual do `utils`, **When** o build é executado após a limpeza, **Then** `bouncycastle`, `snakeyaml` e `mapstruct` não aparecem mais como dependências.
2. **And** `guava` foi substituído por `commons-lang3` no único ponto de uso.
3. **And** a avaliação de substituição de `commons-io` por `java.nio` e da necessidade de `spring-cloud-starter` está documentada com a decisão tomada.

## Tasks / Subtasks

- [ ] Task 1: Remover as 3 dependências com zero uso real (AC: #1)
  - [ ] Remover `<dependency>org.bouncycastle:bcprov-jdk18on</dependency>` de `utils/pom.xml` (linhas 162-165) — 0 classes usam
  - [ ] Remover `<dependency>org.yaml:snakeyaml</dependency>` (linhas 158-161) — 0 classes usam
  - [ ] Remover `<dependency>org.mapstruct:mapstruct</dependency>` (linhas 142-145) — 0 classes usam
  - [ ] Um commit por dependência removida, cada um com build verde (NFR2) — não agrupar as 3 num único commit
- [ ] Task 2: Substituir guava por commons-lang3 (AC: #2)
  - [ ] Localizar o único ponto de uso: `utils/src/main/java/.../enums/StringTransformRule.java`
  - [ ] Substituir a chamada Guava pelo equivalente em `org.apache.commons.lang3` já presente no POM (já é dependência declarada — não é lib nova) ou por código próprio de uma linha, o que for mais curto
  - [ ] Remover `<dependency>com.google.guava:guava</dependency>` (linhas 154-157) após confirmar zero uso restante
- [ ] Task 3: Avaliar e documentar `commons-io` e `spring-cloud-starter` (AC: #3)
  - [ ] Grep pelo único ponto de uso de `commons-io` no módulo; avaliar se `java.nio.file.Files`/`java.nio.file.Path` cobre o caso com uma troca de 1 linha; se cobrir, trocar e remover a dependência; se não, documentar por que não
  - [ ] Grep pelos 3 pontos de uso de `spring-cloud-starter`; confirmar se são reais (import ativo) ou herdados sem uso; documentar a decisão (manter ou remover) com a justificativa
  - [ ] Registrar as duas decisões (mesmo que a decisão seja "manter") em `utils/README.md` ou nas Completion Notes desta story — o AC exige documentação da avaliação, não necessariamente a remoção

## Dev Notes

- Tabela de diagnóstico já levantada no plano de origem (fonte primária, não re-investigar do zero): `bcprov-jdk18on` → 0 usos → remover; `snakeyaml` → 0 → remover; `mapstruct` → 0 → remover; `guava` → 1 uso (`StringTransformRule`) → substituir; `commons-io` → 1 uso → avaliar `java.nio`; `spring-cloud-starter` → 3 usos → verificar necessidade real.
- `commons-lang3` **já é dependência declarada** em `utils/pom.xml` (linha 176) — não adicionar dependência nova, só usar o que já existe.
- **Fora de escopo desta story**: `gson` (Story 1.3), `jackson-dataformat-smile` (vai para `cache` na Story 1.10, não é dependência morta).
- **NFR2** (build verde a cada commit) é o requisito de processo mais importante aqui: a Fase 1 do plano de origem prescreve "um commit por dependência removida, cada um verde" — não faça squash das remoções num commit único.
- Esta é a Fase 1 ("Limpeza") do plano de origem — roda **antes** da extração de qualquer módulo novo, para não carregar peso morto para eles.
- Rodar `mvn dependency:analyze` no módulo `utils` antes de remover, para confirmar que as libs realmente não aparecem no bytecode compilado (não só ausência de `import` textual).

### Project Structure Notes

- Único arquivo tocado: `utils/pom.xml` (remoção de `<dependency>`) e `utils/src/main/java/br/com/sawcunhaos/foundation/utils/enums/StringTransformRule.java` (troca de chamada Guava).
- Nenhum módulo novo é criado nesta story.

### References

- [Source: etc/doc/plano/plano-decomposicao-utils.md#6-limpeza-prévia]
- [Source: etc/doc/plano/plano-decomposicao-utils.md#fase-1--limpeza-1-dia]
- [Source: utils/pom.xml#L142-L177]
- [Source: _bmad-output/planning-artifacts/epics.md#story-12-remover-dependências-mortas-e-obsoletas]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
