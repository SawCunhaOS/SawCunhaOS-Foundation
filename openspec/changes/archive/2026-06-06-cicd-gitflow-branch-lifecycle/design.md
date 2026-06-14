## Context

Modelo de branches do projeto:
- `develop` corre em paralelo — nunca é source de release/fix branches (exceto major)
- Release minor: `release/1.1.0 → release/1.2.0 → release/1.3.0` (cadeia linear)
- Fix sempre nasce junto com a próxima minor: publicar `1.2.0` cria `fix/1.2.1` E `release/1.3.0`
- Fix só existe se sua minor já foi publicada

Cadeia de merge obrigatória:
- FIX: `fix/X.Y.Z` → `release/X.(Y+1).0` → `develop`
- MINOR: `release/X.Y.0` → `develop`
- MAJOR: N/A (já em develop)

## Goals / Non-Goals

**Goals:**
- develop sempre recebe correções da linha de release
- Branches obsoletas não se acumulam
- Bug fix: fix branch nasce do código correto (release, não develop)

**Non-Goals:**
- Merge automático de develop para release (develop é paralelo, não alimenta release)
- Expiração de fix branches antigas (sem prazo definido)
- Alterar workflows do GitHub Actions

## Decisions

**`merge_into` com `-X ours --no-ff`**
`-X ours` resolve todo conflito favorecendo o branch destino (develop, release). POM versions diferem entre branches — develop sempre mantém sua versão SNAPSHOT. `--no-ff` preserva o grafo de merges no histórico.

**Cálculo dinâmico de branches**
- Próxima minor de `fix/X.Y.Z`: `release/X.(Y+1).0`
- Branch anterior de `release/X.Y.0`: `release/X.(Y-1).0`
- Calculados a partir das variáveis `CUR_MAJOR`, `CUR_MINOR`, `CUR_FIX` já existentes no script

**Bug fix: `ORIGINAL_BRANCH` antes de criar release/1.3.0**
O case `minor` atual faz `git checkout develop` antes de criar fix — corrompendo o source. Fix: salvar `ORIGINAL_BRANCH=$(git branch --show-current)` antes de `create_branch_with_snapshot`, e retornar a ele antes de criar o fix.

**Major não muda**
Major parte de develop — não há branch de release para mergear de volta. Comportamento atual preservado.

**Guard: `CUR_MINOR >= 1` antes de deletar branch anterior**
Quando `CUR_MINOR = 0`, `CUR_MINOR - 1 = -1` geraria `release/X.-1.0`. Validar antes.

## Risks / Trade-offs

**`-X ours` resolve TODO conflito pelo destino** → Código conflitante (não só versão) também vai para o destino silenciosamente. Documentado como comportamento esperado — merge de release para develop traz código novo; conflito real indica divergência que deveria ter sido resolvida antes.

**`set -e` com merge parcialmente aplicado** → Se merge falhar, tag e deploy já ocorreram. Estado: tag existe, branches novas não criadas. Recovery manual: executar os passos de merge e criação de branch manualmente após investigar o conflito.

**Branch protegida no GitHub** → Push para `develop` pode falhar se branch protection exigir PR. `PAT_TOKEN` deve ter permissão de bypass ou branch protection deve ser temporariamente relaxada para o bot de release.

## Migration Plan

1. Adicionar funções `merge_into` e `delete_previous_release_branch` no `release.sh`
2. Corrigir bug do source do fix no case `minor` (salvar `ORIGINAL_BRANCH`)
3. Inserir chamadas nas seções `minor` e `fix`
4. Testar localmente em repositório de teste (criar branches de simulação)
5. Fazer release de patch para validar em produção
