## Why

O `release.sh` cria novas branches mas nunca fecha o ciclo anterior: `release/1.1.0` permanece viva após `1.2.0` ser lançada, hotfixes em `fix/*` nunca chegam a `develop`, e há um bug ativo onde a branch `fix/X.Y.Z` é criada a partir de `develop` em vez da `release/X.Y.0` — incluindo código não-lançado de `develop` no hotfix.

## What Changes

- **Merge-back automático** em cadeia hierárquica após release: `fix → próxima minor → develop` e `minor → develop`
- **Deleção de branch anterior** no release minor: `release/X.(Y-1).0` deletada (local + remote) após release de `X.Y.0`
- **Bug fix**: `fix/X.Y.Z` passa a ser criado de `release/X.Y.0` (não de `develop`) no case `minor` do `release.sh`
- Função genérica `merge_into(target, source)` com `-X ours --no-ff` — develop sempre vence conflitos de POM

## Capabilities

### New Capabilities

- `gitflow-merge-back`: Após publicar, o script executa merge em cadeia hierárquica para manter develop sincronizado com a linha de release
- `gitflow-branch-cleanup`: Branch de release anterior é deletada (local + remote) ao publicar nova minor

### Modified Capabilities

*(sem modificação de specs existentes — bug fix e comportamento novo em release.sh)*

## Impact

- `scripts/release.sh`: adição de funções `merge_into` e `delete_previous_release_branch`; chamadas nos cases `minor` e `fix`; bug fix no case `minor` (source do fix branch)
- Sem impacto em workflows do GitHub Actions (lógica fica no script)
- Sem impacto em módulos funcionais, APIs ou contratos
