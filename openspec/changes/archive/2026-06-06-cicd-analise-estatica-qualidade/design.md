## Context

Projeto multi-módulo Maven (privacy, utils, exception, audit, jdempotent, security) com perfil `analyze` já configurado para JaCoCo e OWASP dependency-check. O job `security-check` no `build.yml` existe mas os resultados são invisíveis: `continue-on-error: true` no checkstyle faz falhas serem ignoradas, relatórios JaCoCo nunca são uploadados, SpotBugs está ausente, e o módulo `privacy` sequer é arquivado.

## Goals / Non-Goals

**Goals:**
- SpotBugs visível no CI com relatório por módulo
- JaCoCo report disponível como artefato após cada build
- Checkstyle passa a bloquear (determinístico, sem desculpa para continue-on-error)
- Todos os 6 módulos arquivados corretamente

**Non-Goals:**
- Gate duro de cobertura (fail se < 80%) — decisão futura
- SonarCloud/SonarQube — sem servidor externo
- PMD — SpotBugs já cobre bugs potenciais

## Decisions

**SpotBugs nível MEDIUM+**
Nível HIGH perde muitos problemas relevantes. Nível LOW gera ruído excessivo. MEDIUM é o equilíbrio padrão para projetos de framework/library.

**Excludes via `etc/spotbugs/exclude.xml`**
Lombok e MapStruct geram bytecode que SpotBugs marca como falso positivo (null returns em builders, etc.). O arquivo de excludes filtra por anotação (`@lombok.*`) e por padrão de classe gerada (`*$Builder`, `*$$*`).

**`dependency-check` mantém `continue-on-error: true`**
A NVD API tem histórico documentado de instabilidade e rate-limiting. Falhar o build por indisponibilidade de serviço externo penaliza o desenvolvedor sem motivo. Mantido como informativo.

**`checkstyle` perde `continue-on-error`**
Checkstyle roda localmente com resultado determinístico — não há infra externa. Um checkstyle que nunca bloqueia é um checkstyle inútil.

**Upload de artefatos com `if: always()`**
Relatórios de JaCoCo e SpotBugs são mais úteis quando um step falha (para diagnóstico). `if: always()` garante upload mesmo em caso de falha no check.

## Risks / Trade-offs

**SpotBugs lento em multi-módulo** → Mitigado: roda apenas no perfil `analyze`, separado do build normal. Se ultrapassar 20min, adicionar `-Dspotbugs.effort=less` como ajuste.

**Checkstyle bloqueante pode quebrar PRs existentes** → Verificar regras do checkstyle atual antes de remover `continue-on-error`. Se houver violações acumuladas, fazer cleanup em commit separado antes desta mudança.

**Falsos positivos SpotBugs** → Mitigado pelo `exclude.xml`. Novos falsos positivos identificados em runtime podem ser adicionados ao arquivo sem reconfigurar o plugin.

## Migration Plan

1. Criar `etc/spotbugs/exclude.xml` com excludes para Lombok/MapStruct
2. Adicionar plugin ao `pom.xml` perfil `analyze`
3. Rodar `mvn -Panalyze spotbugs:check` localmente para validar tempo e falsos positivos
4. Atualizar `build.yml`: archive privacy, upload artefatos, remover continue-on-error checkstyle
5. Abrir PR — o próprio CI valida a mudança

Rollback: reverter `build.yml` e `pom.xml`. Sem impacto em produção (CI-only).
