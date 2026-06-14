## ADDED Requirements

### Requirement: Notas de release geradas por git-cliff agrupadas por módulo
O workflow `release-notes.yml` SHALL executar `git-cliff --latest` (config `cliff.toml`) ao detectar push de tag com formato `*.*.*`, gerando notas em markdown agrupadas por scope (módulo) do commit. As notas geradas SHALL ser usadas como corpo do GitHub Release. O `CHANGELOG.md` NÃO é commitado nem versionado pelo workflow.

#### Scenario: Notas geradas após release minor
- **WHEN** a tag `1.2.0` é criada e pushed por `release.sh`
- **THEN** o workflow `release-notes.yml` executa e gera notas da versão 1.2.0 com entradas agrupadas por scope

#### Scenario: Commits com scope agrupados por módulo
- **WHEN** commits `feat(audit): batch pipeline` e `fix(privacy): masking null` existem desde a tag anterior
- **THEN** as notas geradas contêm seção `### audit` com o feat e seção `### privacy` com o fix

#### Scenario: Commits sem scope aparecem em seção Outros
- **WHEN** commit `feat: melhoria geral` existe sem scope definido
- **THEN** as notas geradas contêm seção `### Outros` com o commit
