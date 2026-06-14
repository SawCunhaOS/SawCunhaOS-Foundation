## 1. Script de Validação

- [x] 1.1 Criar `scripts/validate-pr-target.sh` com leitura de `HEAD_REF` e `BASE_REF` do ambiente
- [x] 1.2 Implementar parsing de tipo de branch: regex match para `feature/*`, `fix/X.Y.Z`, `release/X.Y.0`; default pass-through para não reconhecidos
- [x] 1.3 Implementar extração de versão de `fix/X.Y.Z`: `IFS='.' read -r MAJOR MINOR FIX <<< "${head#fix/}"` para calcular `next_fix=fix/$MAJOR.$MINOR.$((FIX+1))` e `next_minor=release/$MAJOR.$((MINOR+1)).0`
- [x] 1.4 Implementar extração de versão de `release/X.Y.0`: extrair MAJOR e MINOR para calcular `next_minor=release/$MAJOR.$((MINOR+1)).0`
- [x] 1.5 Implementar verificação de existência remota do `next_fix`: `gh api "repos/$GITHUB_REPOSITORY/branches/$next_fix" --silent 2>/dev/null && echo true || echo false`; usar `|| true` no comando para não falhar por rate limit
- [x] 1.6 Implementar validação do case `fix`: `BASE ∈ {next_fix [se existir], next_minor}` → pass; else → fail com mensagem
- [x] 1.7 Implementar validação do case `release`: `BASE ∈ {develop, next_minor}` → pass; else → fail com mensagem
- [x] 1.8 Mensagem de falha deve incluir: branch de origem, base tentada, lista de bases permitidas
- [x] 1.9 Testar localmente: `HEAD_REF=fix/1.1.2 BASE_REF=develop GITHUB_REPOSITORY=owner/repo bash scripts/validate-pr-target.sh` deve falhar _(testado + 9 cenários extras das specs, todos OK)_

## 2. Workflow

- [x] 2.1 Criar `.github/workflows/validate-pr-target.yml` com trigger `on: pull_request: types: [opened, edited, synchronize, reopened]`
- [x] 2.2 Adicionar job `validate-target` com `runs-on: ubuntu-latest`
- [x] 2.3 Passar `HEAD_REF: ${{ github.head_ref }}`, `BASE_REF: ${{ github.base_ref }}`, `GITHUB_REPOSITORY: ${{ github.repository }}` como env para o step de script
- [x] 2.4 Usar `GITHUB_TOKEN` padrão para o step (sem PAT — apenas leitura de branches)

## 3. Configuração de Branch Protection

- [x] 3.1 No GitHub: Settings → Branches → adicionar regra para `release/*`; adicionar `validate-pr-target` como required status check
- [x] 3.2 Repetir para `fix/*` e `develop`
- [x] 3.3 Testar: abrir PR de `fix/X.Y.Z → develop` e confirmar que o check bloqueia
- [x] 3.4 Testar: abrir PR de `fix/X.Y.Z → release/X.(Y+1).0` e confirmar que o check passa
