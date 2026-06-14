## Context

Três workflows independentes hoje: `build.yml` (testa), `publish-snapshot.yml` (publica), `manual-release.yml` (release). O publish não tem dependência do build — risco real de snapshot quebrado no repositório. GPG setup é copiado literalmente entre publish e release (14 linhas duplicadas).

## Goals / Non-Goals

**Goals:**
- Snapshot nunca publicado antes de build+testes verdes
- Scripts com pré-condições explícitas (versão, working tree)
- GPG setup em um único lugar

**Non-Goals:**
- Rodar testes novamente no deploy (testes já rodaram no build)
- Mudar a lógica de versionamento dos scripts
- Alterar o fluxo do `manual-release.yml` além da composite action

## Decisions

**`workflow_run` como trigger do publish-snapshot**
`workflow_run` é o mecanismo do GitHub para dependência cross-workflow. Permite que `publish-snapshot.yml` execute somente quando `build.yml` completar com sucesso na mesma branch. Alternativa `needs` (mesmo workflow) não se aplica — são workflows separados por design.

**Branch filter no `workflow_run`**
O trigger deve filtrar as mesmas branches do publish atual: `develop`, `fix/**/*`, `release/**/*`. Sem filtro, o workflow executaria para qualquer branch onde build passe.

**Validação de SNAPSHOT no script (não no YAML)**
A validação em shell é executável localmente, útil para debugging. YAML-only seria invisível ao rodar scripts manualmente.

**Bypass `RELEASE_DEPLOY=1` para o release.sh**
`deploy.sh` é compartilhado entre o publish de SNAPSHOT (`publish-snapshot.yml`) e o deploy de release (chamado por `release.sh` após `close_version.sh` strippar o `-SNAPSHOT`). Um guard SNAPSHOT incondicional quebraria o `release.sh`. Solução: o guard só bloqueia quando `RELEASE_DEPLOY` não é `1`; o `release.sh` chama `RELEASE_DEPLOY=1 ./scripts/deploy.sh`. Humano rodando `deploy.sh` direto numa versão de release continua bloqueado com a mensagem orientando a usar o `release.sh`.

**`-DskipTests` mantido no deploy**
Testes rodaram no `build.yml`. Rodar novamente consome ~5-10min por nada. O `workflow_run` garante que o deploy só acontece após testes verdes.

**Composite action (não reusable workflow)**
GPG setup é um bloco de steps, não um job completo. Composite action não consome runner extra e funciona dentro do contexto do job caller.

## Risks / Trade-offs

**`workflow_run` usa código do branch padrão para o workflow consumidor** → O YAML do `publish-snapshot.yml` executado é sempre o do branch padrão (develop), não do branch que disparou o build. Mudanças no workflow precisam estar em develop para ter efeito imediato.

**`workflow_run` não dispara para forks** → Sem impacto (projeto single-owner, sem forks esperados).

**Validação de working tree sempre passa em CI** → Checkout do CI é sempre limpo. A validação é útil principalmente localmente. Mantida por consistência e segurança.

## Migration Plan

1. Criar `.github/actions/setup-gpg/action.yml`
2. Atualizar `manual-release.yml` para usar composite action (validação: release GPG ainda funciona)
3. Atualizar `publish-snapshot.yml`: trigger `workflow_run` + composite action + validação SNAPSHOT
4. Atualizar `deploy.sh` e `release.sh` com guards
5. Testar push em develop → verificar que `publish-snapshot.yml` dispara após `build.yml`

Rollback: reverter triggers do `publish-snapshot.yml` para `push`. Scripts têm `set -e` — falha de validação é explícita.
