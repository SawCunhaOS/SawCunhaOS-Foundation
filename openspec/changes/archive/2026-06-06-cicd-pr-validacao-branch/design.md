## Context

Matriz de branches permitidos espelha a cadeia de merge do `release.sh`:
- `feature/*` → qualquer branch (sem restrição)
- `fix/X.Y.Z` → `fix/X.Y.(Z+1)` (se existir) ou `release/X.(Y+1).0`
- `release/X.Y.0` → `develop` ou `release/X.(Y+1).0` (pós-major: develop avançou de major, releases da série anterior vão para a próxima minor)

## Goals / Non-Goals

**Goals:**
- PRs com target inválido bloqueadas antes do merge
- Mensagem de erro clara com targets permitidos
- Script testável localmente

**Non-Goals:**
- Validar conteúdo da PR (template) — ideia separada
- Bloquear PRs de bots/dependabot
- Escape hatch por label

## Decisions

**Script shell externo (não YAML inline)**
Lógica de parsing de branch name e chamada de API em shell é testável localmente com variáveis de ambiente mock. YAML inline é opaco e difícil de debugar.

**`gh api` para verificar existência de next_fix**
`gh api repos/:owner/:repo/branches/:branch` retorna 200 se existe, 404 se não. `|| true` no comando garante que falha de rede não bloqueia PR legítima — assume-se que `next_fix` não existe e só `next_minor` é permitido.

**Pass-through para branches não reconhecidos**
Branches como `dependabot/*`, `renovate/*`, `chore/*` não casam com nenhum padrão → check retorna success automaticamente. Evita quebrar fluxos de automação.

**`feature/*` sem restrição de destino**
Feature branches são flexíveis por design — podem ser direcionadas a qualquer branch conforme necessidade da sprint.

## Risks / Trade-offs

**PR editada depois de branch protection configurada** → Check roda novamente em `edited` e `synchronize`. PR é desbloqueada automaticamente ao corrigir o target.

**`gh api` rate limit** → Com `|| true`, a falha de API faz o check assumir `next_fix` ausente — PR ainda pode ir para `next_minor`. Comportamento seguro.

**`release/X.Y.0` com patch != 0** → Branches de release do projeto sempre terminam em `.0`. Se existir `release/1.2.1` (não esperado), o parsing extrai `.1` como patch e calcula `next_minor = release/1.3.0` corretamente.

## Migration Plan

1. Criar `scripts/validate-pr-target.sh`
2. Testar localmente com variáveis HEAD_REF/BASE_REF mockadas
3. Criar `.github/workflows/validate-pr-target.yml`
4. Configurar required status check no GitHub (manual: Settings → Branches → Branch protection rules)
5. Abrir PR de teste com target inválido para validar bloqueio
