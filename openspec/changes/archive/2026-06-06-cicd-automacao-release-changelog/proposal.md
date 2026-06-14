## Why

O `CHANGELOG.md` é mantido manualmente e frequentemente esquecido. Quando `release.sh` cria e faz push de uma tag, nenhum GitHub Release é criado automaticamente — o histórico de releases existe apenas nas tags do git sem visualização rica. Commits usam `feat:`/`fix:` sem scope, impossibilitando agrupamento automático por módulo.

## What Changes

- **`cliff.toml`** criado na raiz com configuração de tag pattern, commit parsers por tipo/scope e template Tera que agrupa por módulo (scope)
- **Workflow `release-notes.yml`** criado, disparado em push de tags `*.*.*` — executa git-cliff e cria/atualiza GitHub Release com notas ricas (o `CHANGELOG.md` não é commitado pelo workflow)
- **Convenção de commits com scope** documentada em `etc/doc/commit-convention.md` — `feat(audit):`, `fix(exception):` etc.
- Commits anteriores sem scope aparecem em seção "Outros" — sem reprocessamento do histórico

## Capabilities

### New Capabilities

- `changelog-automatizado`: Geração automática de notas de release via git-cliff ao publicar uma tag, com entradas agrupadas por módulo (scope do commit) — usadas no GitHub Release, sem commit de `CHANGELOG.md`
- `github-release-automatico`: Criação automática de GitHub Release com release notes ricas em markdown ao publicar uma tag, sem interação manual
- `convencao-commits-scope`: Convenção de commits com scope por módulo documentada e adotada a partir desta mudança

### Modified Capabilities

*(sem modificação de specs existentes)*

## Impact

- `cliff.toml`: novo arquivo na raiz
- `.github/workflows/release-notes.yml`: novo workflow
- `etc/doc/commit-convention.md`: novo documento de convenção
- `CHANGELOG.md`: não é alterado pelo workflow; permanece manual (workflow só gera notas para o GitHub Release)
- Sem impacto em módulos funcionais, APIs ou scripts de release existentes
