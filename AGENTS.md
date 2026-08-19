<!-- bmad:context -->
<!-- Verified 2026-08-19 against 094871c. Managed by bmad-project-context; edits inside this block are replaced on refresh. Keep anything you want preserved outside the markers. -->

## SawCunhaOS-Foundation

Biblioteca fundamental Java 25 / Spring Boot para o ecossistema SCOS, multi-módulo Maven (`privacy`, `utils`, `exception`, `audit`, `jdempotent`). Consumida por uma base piloto/restrita (ver PRD da 1.2.0). Planejamento e notas técnicas em `etc/doc/ideia/` e `etc/doc/plano/`; convenção de commits em `etc/doc/commit-convention.md`.

## Policy

- Nunca faça commit ou push sem liberação explícita do humano.
- PRs: o branch de destino é validado por `scripts/validate-pr-target.sh` no CI — confira antes de propor um alvo (ex.: `fix/X.Y.Z` → `release/X.(Y+1).0`; `release/X.Y.Z` → `develop` ou `release/X.(Y+1).0`).

## Where things are

- Convenção de commits (scope = módulo Maven, tipo → seção do CHANGELOG): `etc/doc/commit-convention.md`.
- Skills de configuração por módulo, para projetos consumidores: `etc/doc/skills/`. `scos-security-config` é órfã — documenta o módulo `security`, removido no commit `28f9fca`; não usar como referência.
- Diagrama ER e changelogs Liquibase do módulo `audit`: `etc/audit/database.md`.
- Especificação da release 1.2.0 (PRD, Architecture Spine, Epics/Stories): `_bmad-output/planning-artifacts/` — `prds/prd-SawCunhaOS-Foundation-2026-08-18/prd.md`, `architecture/architecture-SawCunhaOS-Foundation-2026-08-19/ARCHITECTURE-SPINE.md`, `epics.md`.

## Running and verifying

- `mvn test` não roda checkstyle/spotbugs/dependency-check — CI usa o profile `analyze` (`mvn -Panalyze checkstyle:check`, `spotbugs:check`, `dependency-check:check`, `jacoco:report`).
- Testes de `audit` e `jdempotent` usam Testcontainers — suba o Docker antes de `mvn test`, senão falham (ou travam) sem erro claro.

<!-- /bmad:context -->
