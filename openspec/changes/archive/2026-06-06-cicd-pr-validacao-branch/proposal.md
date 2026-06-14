## Why

Não há nada impedindo um desenvolvedor de abrir uma PR de `fix/1.1.2` diretamente para `develop`, pulando a cadeia de merge obrigatória. O erro só seria detectado na publicação — tarde demais. As mesmas regras do `release.sh` devem ser enforçadas na abertura de PRs.

## What Changes

- **Workflow `validate-pr-target.yml`** criado, disparado em `pull_request` (opened, edited, synchronize, reopened)
- **Script `scripts/validate-pr-target.sh`** com lógica de validação de branch target por tipo de branch de origem
- Regras: `feature/*` pode qualquer branch; `fix/X.Y.Z` apenas `fix/X.Y.(Z+1)` (se existir) ou `release/X.(Y+1).0`; `release/X.Y.0` apenas `develop` ou `release/X.(Y+1).0`
- Check é **required status check** — bloqueia merge se falhar
- PRs de bots automatizados são ignoradas

## Capabilities

### New Capabilities

- `pr-validacao-branch-target`: Workflow de CI que valida o branch de destino de PRs segundo a hierarquia de merge do gitflow, bloqueando merges inválidos antes que aconteçam

### Modified Capabilities

*(sem modificação de specs existentes)*

## Impact

- `.github/workflows/validate-pr-target.yml`: novo workflow
- `scripts/validate-pr-target.sh`: novo script
- Branch protection rules (configuração manual): adicionar `validate-pr-target` como required status check
- Sem impacto em módulos funcionais, APIs ou scripts de release
