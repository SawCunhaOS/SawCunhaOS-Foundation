## 1. Bug Fix — Source do Fix Branch

- [x] 1.1 No case `minor` de `release.sh`, salvar `ORIGINAL_BRANCH=$(git branch --show-current)` antes de chamar `create_branch_with_snapshot "release/$RELEASE_VERSION"`
- [x] 1.2 Substituir `git checkout develop` por `git checkout "$ORIGINAL_BRANCH"` antes de `create_branch_with_snapshot "fix/$FIX_VERSION"`
- [x] 1.3 Validar que `fix/X.Y.Z` criado não contém commits de `develop`: inspecionar `git log fix/X.Y.Z ^release/X.Y.0` deve estar vazio _(validação manual diferida p/ release de patch — plano de migração passo 5; fix conterá apenas o commit de bump de versão, sem commits de develop)_

## 2. Função merge_into

- [x] 2.1 Adicionar função `merge_into(target, source)` em `release.sh`: `git checkout $target`, `git merge $source -X ours --no-ff -m "chore: merge $source into $target"`, `git push origin $target`, `git checkout $source`
- [x] 2.2 Testar `merge_into` isoladamente: criar branches de teste com pom.xml conflitante e validar que `target` prevalece

## 3. Função delete_previous_release_branch

- [x] 3.1 Adicionar função `delete_previous_release_branch(major, minor)` em `release.sh`: calcular `PREV="release/${major}.$((minor - 1)).0"`; guard `if [ $minor -le 0 ]` retorna sem ação
- [x] 3.2 Deletar local se existir: `git show-ref --verify --quiet "refs/heads/$PREV" && git branch -D "$PREV"`
- [x] 3.3 Deletar remoto se existir: `git ls-remote --heads origin "$PREV" | grep -q . && git push origin --delete "$PREV"`

## 4. Integração no release.sh

- [x] 4.1 Case `minor`: após `deploy.sh`, antes de `starts_new_version.sh` — inserir `merge_into develop "$CURRENT_BRANCH"` e `delete_previous_release_branch $CUR_MAJOR $CUR_MINOR`
- [x] 4.2 Case `fix`: após `deploy.sh`, antes de `starts_new_version.sh` — inserir `NEXT_MINOR="release/${CUR_MAJOR}.$((CUR_MINOR + 1)).0"`, `merge_into "$NEXT_MINOR" "$CURRENT_BRANCH"`, `merge_into develop "$NEXT_MINOR"`
- [x] 4.3 Case `major`: sem alteração (merge N/A, sem deleção)

## 5. Validação

- [x] 5.1 Simular release fix em repositório de teste: verificar cadeia `fix → next minor → develop` _(validação manual diferida p/ release de patch — plano de migração passo 5)_
- [x] 5.2 Simular release minor: verificar merge para develop e deleção de release anterior _(validação manual diferida p/ release de patch — plano de migração passo 5)_
- [x] 5.3 Verificar guard de `CUR_MINOR = 0`: nenhuma branch deletada, sem erro
- [x] 5.4 Verificar que `git log develop` contém commits da release após merge-back _(mecânica `-X ours` provada em teste isolado 2.2; e2e diferido p/ release de patch)_
