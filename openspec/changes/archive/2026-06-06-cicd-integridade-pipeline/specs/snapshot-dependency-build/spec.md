## ADDED Requirements

### Requirement: Snapshot publicado somente após build verde
O workflow `publish-snapshot.yml` SHALL usar trigger `workflow_run` com `workflows: ["Build & Test"]` e `types: [completed]`, executando somente quando o status de conclusão for `success` e o branch for `develop`, `fix/**/*` ou `release/**/*`.

#### Scenario: Snapshot publicado após build com sucesso
- **WHEN** um push em `develop` dispara `build.yml` e o job completa com sucesso
- **THEN** `publish-snapshot.yml` é disparado automaticamente e publica o snapshot

#### Scenario: Snapshot não publicado após build com falha
- **WHEN** um push em `develop` dispara `build.yml` e o job falha nos testes
- **THEN** `publish-snapshot.yml` não é disparado
