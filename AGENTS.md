<!-- bmad:context -->
<!-- Verified 2026-08-29 against 4dcab58. Managed by bmad-project-context; edits inside this block are replaced on refresh. Keep anything you want preserved outside the markers. -->

## SawCunhaOS-Foundation

Biblioteca fundamental Java 25 / Spring Boot para o ecossistema SCOS, multi-módulo Maven (módulos em `pom.xml`). Consumida por uma base piloto/restrita (ver PRD da 1.2.0). Planejamento e notas técnicas em `etc/doc/ideia/` e `etc/doc/plano/`; convenção de commits em `etc/doc/commit-convention.md`.

## Policy

- Nunca faça commit ou push sem liberação explícita do humano.
- PRs: o branch de destino é validado por `scripts/validate-pr-target.sh` no CI — confira antes de propor um alvo (ex.: `fix/X.Y.Z` → `release/X.(Y+1).0`; `release/X.Y.Z` → `develop` ou `release/X.(Y+1).0`).

## Where things are

- Convenção de commits (scope = módulo Maven, tipo → seção do CHANGELOG): `etc/doc/commit-convention.md`.
- Skills de configuração por módulo, para projetos consumidores: `etc/doc/skills/`. Órfãs (documentam módulos removidos do reactor, não usar como referência): `scos-security-config`, `scos-utils-config`, `scos-exception-config`.
- Diagrama ER e changelogs Liquibase do módulo `audit`: `etc/audit/database.md`.
- Especificação da release 1.2.0 (PRD, Architecture Spine, Epics/Stories): `_bmad-output/planning-artifacts/` — `prds/prd-SawCunhaOS-Foundation-2026-08-18/prd.md`, `architecture/architecture-SawCunhaOS-Foundation-2026-08-19/ARCHITECTURE-SPINE.md`, `epics.md`.
- Módulos com AGENTS.md próprio: `audit/AGENTS.md`, `privacy/AGENTS.md`, `web/AGENTS.md`, `archtest/AGENTS.md`.

## Running and verifying

- `mvn test` não roda checkstyle/spotbugs/dependency-check — CI usa o profile `analyze` (`mvn -Panalyze checkstyle:check`, `spotbugs:check`, `dependency-check:check`, `jacoco:report`).
- Testes de `audit` e `jdempotent` usam Testcontainers — suba o Docker antes de `mvn test`, senão falham (ou travam) sem erro claro.

## Conventions that differ from defaults

- Beans de módulos de biblioteca (`audit`, `jdempotent`, `web`, `privacy`) são registrados via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, não por `@ComponentScan` da app consumidora — um bean novo com só `@Component` nunca é criado numa aplicação real. Bug real pego em revisão antes do merge, Story 1.15 (commit `d97e006`).

<!-- /bmad:context -->
