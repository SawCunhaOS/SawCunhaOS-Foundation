## ADDED Requirements

### Requirement: Relatório JaCoCo publicado como artefato de CI
O job `security-check` no `build.yml` SHALL fazer upload do relatório HTML de cobertura JaCoCo como artefato do GitHub Actions com retention de 7 dias após cada execução, mesmo quando outros steps falharem.

#### Scenario: Relatório disponível após push em qualquer branch
- **WHEN** um push é feito em qualquer branch e o job `security-check` executa
- **THEN** o artefato `jacoco-report` está disponível para download na UI do GitHub Actions contendo o relatório HTML por módulo

#### Scenario: Relatório reflete cobertura real dos módulos
- **WHEN** o relatório JaCoCo é gerado via `mvn -Panalyze jacoco:report`
- **THEN** o artefato contém relatórios individuais para cada módulo (privacy, utils, exception, audit, jdempotent, security)

### Requirement: Módulo privacy incluído no archive de artefatos
O step `Archive artifacts` do job `build` SHALL incluir o diretório `privacy/target/` para que os artefatos compilados do módulo privacy sejam preservados junto aos demais módulos.

#### Scenario: Artefatos de privacy disponíveis após build
- **WHEN** o job `build` completa com sucesso
- **THEN** o artefato `maven-build-privacy-25` está disponível para download na UI do GitHub Actions

### Requirement: Checkstyle bloqueia build quando violações existem
O step `checkstyle:check` no job `security-check` SHALL falhar o build quando encontrar violações, sem `continue-on-error`. O step de `dependency-check` SHALL manter `continue-on-error: true` por depender de NVD API externa.

#### Scenario: Violação de checkstyle bloqueia o job
- **WHEN** `mvn -Panalyze checkstyle:check` encontra violação de estilo
- **THEN** o job `security-check` falha e o merge da PR é bloqueado pelo required status check

#### Scenario: Falha do dependency-check não bloqueia o job
- **WHEN** a NVD API está indisponível e `dependency-check:check` falha
- **THEN** o job `security-check` continua e completa com sucesso (continue-on-error preservado)
