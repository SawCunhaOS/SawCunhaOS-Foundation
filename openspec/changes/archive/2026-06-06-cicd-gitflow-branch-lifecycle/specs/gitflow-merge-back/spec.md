## ADDED Requirements

### Requirement: Release minor executa merge-back para develop
Após publicar uma release minor, o `release.sh` SHALL executar `git merge release/X.Y.0 → develop` com estratégia `-X ours --no-ff` e mensagem `chore: merge release/X.Y.0 into develop`, antes de criar as novas branches.

#### Scenario: develop recebe código da minor após release
- **WHEN** `release.sh minor` completa o deploy e tag da versão `1.2.0`
- **THEN** `develop` recebe um merge commit de `release/1.2.0` com os commits da release incorporados

#### Scenario: Conflito de POM resolvido automaticamente
- **WHEN** `develop` tem `2.0.0-SNAPSHOT` e `release/1.2.0` tem `1.2.0`, gerando conflito no pom.xml
- **THEN** o merge completa com a versão do `develop` (`2.0.0-SNAPSHOT`) prevalecendo no pom.xml

### Requirement: Release fix executa merge-back em cadeia hierárquica
Após publicar um fix, o `release.sh` SHALL executar: (1) `git merge fix/X.Y.Z → release/X.(Y+1).0` e (2) `git merge release/X.(Y+1).0 → develop`, ambos com `-X ours --no-ff`, antes de criar o próximo fix branch.

#### Scenario: Fix propagado para próxima minor e depois para develop
- **WHEN** `release.sh fix` completa o deploy e tag da versão `1.1.2`
- **THEN** `release/1.2.0` recebe merge de `fix/1.1.2`, e em seguida `develop` recebe merge de `release/1.2.0`

### Requirement: Bug fix — fix branch criado de release branch, não de develop
No case `minor` do `release.sh`, o branch `fix/X.Y.Z` SHALL ser criado a partir do `release/X.Y.0` atual (salvo em `ORIGINAL_BRANCH` antes de `create_branch_with_snapshot`), não de `develop`.

#### Scenario: fix/1.2.1 criado sem código não-lançado de develop
- **WHEN** `release.sh minor` é executado em `release/1.2.0`
- **THEN** `fix/1.2.1` é criado a partir de `release/1.2.0` e não contém commits de `develop` que ainda não foram lançados
