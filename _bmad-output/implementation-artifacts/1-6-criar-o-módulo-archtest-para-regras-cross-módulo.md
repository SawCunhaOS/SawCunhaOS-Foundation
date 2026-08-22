# Story 1.6: Criar o módulo `archtest` para regras cross-módulo

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como mantenedor do `scos-foundation`,
Eu quero um módulo de teste dedicado para regras ArchUnit que citam mais de um módulo,
Para que nenhuma regra cross-módulo fique duplicada ou ausente.

## Acceptance Criteria

1. **Given** que nenhum módulo de implementação enxerga o classpath inteiro sozinho, **When** o módulo `archtest` é criado (`scope=test`, sem código de produção, dependendo de todos os outros só para teste), **Then** ele compila e roda no perfil `analyze` do build do reactor.
2. **And** o módulo está pronto para receber a primeira regra cross-módulo nas stories seguintes.

## Tasks / Subtasks

- [ ] Task 1: Criar o módulo Maven `scos-foundation-archtest` (AC: #1)
  - [ ] `scope=test` — nenhuma classe de produção, só `src/test/java`
  - [ ] Depender, em escopo `test`, de todos os módulos já existentes no reactor neste ponto (`privacy`, `utils`, `exception`, `audit`, `jdempotent` — e, se já criados por esta altura da sequência, `audit-api`/`jdempotent-api`/`validation-api` das Stories 1.5): só assim consegue enxergar o classpath inteiro para checar regras cross-módulo
  - [ ] Usar a dependência `com.tngtech.archunit:archunit-junit5:1.5.0` já gerenciada em `pluginManagement`/`dependencyManagement` do pai (Story 1.4) — declarar sem versão
  - [ ] Ativar o perfil `analyze` herdado do pai (Story 1.4) para que `archtest` rode dentro do mesmo gate mecânico dos demais módulos
- [ ] Task 2: README do módulo (AC: #2)
  - [ ] Explicar o propósito: hospedar regras ArchUnit que citam mais de um módulo (ex.: "nada depende de `web`", "nenhum ciclo entre módulos") — regras que dizem respeito a um módulo só continuam nascendo dentro do próprio módulo
  - [ ] Deixar uma seção "regras existentes" vazia/placeholder mínima — a primeira regra real chega só na Story 1.14
- [ ] Task 3: Adicionar `archtest` em `<modules>` do `pom.xml` raiz (AC: #1)

## Dev Notes

- Este módulo nasce **vazio de regras** — só a estrutura. As regras cross-módulo (ex.: "nenhum módulo além de aplicações depende de `web`", "sem ciclo entre módulos") são escopo da Story 1.14, depois que os módulos folha existirem. Não antecipar regras aqui — a AC #2 pede só que o módulo esteja "pronto para receber", não que já receba.
- Regras que citam um módulo só (ex.: "`core` não importa Spring") **não** entram aqui — nascem dentro do próprio módulo protegido, no mesmo commit que o cria (AD-5). `archtest` é exclusivamente para regras que nenhum módulo individual consegue verificar sozinho.
- Depende da Story 1.4 (perfil `analyze` e ArchUnit já gerenciados no pai) estar concluída antes.
- Sem dependência circular: `archtest` depende de tudo (escopo teste), nada depende de `archtest`.

### Project Structure Notes

- Módulo Maven novo: `archtest/` — `pom.xml`, `README.md`, `src/test/java/` vazio (estrutura pronta, sem classe de teste real ainda).
- `pom.xml` raiz ganha `<module>archtest</module>`.

### References

- [Source: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Foundation-2026-08-19/ARCHITECTURE-SPINE.md#ad-5--archunit-como-mecanismo-de-imposição-das-regras-de-módulo-adopted]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Foundation-2026-08-19/GUIA-DE-IMPLEMENTACAO.md#3-archtest--o-módulo-novo-que-a-spine-adicionou]
- [Source: _bmad-output/planning-artifacts/epics.md#story-16-criar-o-módulo-archtest-para-regras-cross-módulo]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
