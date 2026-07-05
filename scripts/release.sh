#!/usr/bin/env bash


#
# /*
#  * Copyright 2026 SawCunha Open System - SawCunhaOS-Foundation
#  *
#  * Licensed under the Apache License, Version 2.0 (the "License");
#  * you may not use this file except in compliance with the License.
#  * You may obtain a copy of the License at
#  *
#  *     http://www.apache.org/licenses/LICENSE-2.0
#  */
#

set -e

TYPE="$1"

if [[ -z "$TYPE" ]]; then
  echo "Uso: ./release.sh [major|minor|fix]"
  exit 1
fi

# Pré-condição: working tree limpo antes de qualquer operação
if [[ -n "$(git status --porcelain)" ]]; then
  echo "Erro: working tree não está limpo. Faça commit ou stash das mudanças antes do release."
  exit 1
fi

CURRENT_BRANCH=$(git branch --show-current)
echo "Branch atual: $CURRENT_BRANCH"

# Validação de branch por tipo
case "$TYPE" in
  major)
    if [[ "$CURRENT_BRANCH" != "develop" ]]; then
      echo "Erro: Major só pode ser fechada na branch develop"
      exit 1
    fi
    ;;
  minor)
    if [[ "$CURRENT_BRANCH" != release/* ]]; then
      echo "Erro: Minor só pode ser fechada em branch release/*"
      exit 1
    fi
    ;;
  fix)
    if [[ "$CURRENT_BRANCH" != fix/* ]]; then
      echo "Erro: Fix só pode ser fechada em branch fix/*"
      exit 1
    fi
    ;;
  *)
    echo "Tipo inválido: $TYPE"
    exit 1
    ;;
esac

# Versão atual antes do fechamento
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
BASE_VERSION=${CURRENT_VERSION/-SNAPSHOT/}
IFS='.' read -r CUR_MAJOR CUR_MINOR CUR_FIX <<< "$BASE_VERSION"

echo "Versão atual: $BASE_VERSION"

# Merge hierárquico de source -> target. develop/release sempre vence conflitos
# (estratégia -X ours no destino); --no-ff preserva o grafo de merges.
merge_into() {
  local TARGET=$1
  local SOURCE=$2
  echo "==> Merge-back: $SOURCE -> $TARGET"
  git checkout "$TARGET"
  git merge "$SOURCE" -X ours --no-ff -m "chore: merge $SOURCE into $TARGET"
  git push origin "$TARGET"
  git checkout "$SOURCE"
}

# Deleta a branch de release anterior (local + remota) ao publicar nova minor.
# Silenciosa se a branch não existir; guard contra release/X.-1.0 quando minor=0.
delete_previous_release_branch() {
  local MAJOR=$1
  local MINOR=$2
  if [ "$MINOR" -le 0 ]; then
    echo "==> Nenhuma branch de release anterior a deletar (minor=$MINOR)"
    return 0
  fi
  local PREV="release/${MAJOR}.$((MINOR - 1)).0"
  if git show-ref --verify --quiet "refs/heads/$PREV"; then
    echo "==> Deletando branch local $PREV"
    git branch -D "$PREV"
  fi
  if git ls-remote --heads origin "$PREV" | grep -q .; then
    echo "==> Deletando branch remota $PREV"
    git push origin --delete "$PREV"
  fi
}

echo "==> Fechando versão"
./scripts/close_version.sh

echo "==> Criando tag $BASE_VERSION"
git tag -a "$BASE_VERSION" -m "Release $BASE_VERSION"
git push origin "$BASE_VERSION"

echo "==> Realizando deploy"
RELEASE_DEPLOY=1 ./scripts/deploy.sh

# Merge-back hierárquico + limpeza de branch anterior (antes de abrir nova versão).
# close_version.sh deixa o POM da release não-commitado; commitamos aqui para que
# o merge_into possa trocar de branch com a árvore limpa.
case "$TYPE" in
  minor)
    if ! git diff --quiet || ! git diff --cached --quiet; then
      git add .
      git commit -m "chore: release $BASE_VERSION"
    fi
    merge_into develop "$CURRENT_BRANCH"
    delete_previous_release_branch "$CUR_MAJOR" "$CUR_MINOR"
    ;;
  fix)
    if ! git diff --quiet || ! git diff --cached --quiet; then
      git add .
      git commit -m "chore: release $BASE_VERSION"
    fi
    NEXT_MINOR="release/${CUR_MAJOR}.$((CUR_MINOR + 1)).0"
    merge_into "$NEXT_MINOR" "$CURRENT_BRANCH"
    merge_into develop "$NEXT_MINOR"
    git checkout "$CURRENT_BRANCH" # volta ao fix branch p/ abrir próxima versão
    ;;
  major)
    : # major já está em develop — sem merge-back nem deleção de release
    ;;
esac

echo "==> Abrindo nova versão ($TYPE)"
./scripts/starts_new_version.sh "$TYPE"

# Nova versão após abertura
NEW_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
NEW_BASE_VERSION=${NEW_VERSION/-SNAPSHOT/}

# Função para criar branch, atualizar POM multi-módulo e commit
create_branch_with_snapshot() {
  local BRANCH_NAME=$1
  local VERSION=$2

  # Remove branch antiga
  if git show-ref --verify --quiet "refs/heads/$BRANCH_NAME"; then
    echo "==> Removendo branch antiga local $BRANCH_NAME"
    git branch -D "$BRANCH_NAME"
    echo "==> Removendo branch antiga remota $BRANCH_NAME"
    git push origin --delete "$BRANCH_NAME"
  fi

  echo "==> Criando branch $BRANCH_NAME"
  git checkout -b "$BRANCH_NAME"

  echo "==> Atualizando POM para $VERSION-SNAPSHOT em todos os módulos"
  mvn versions:set -DnewVersion="${VERSION}-SNAPSHOT" -DgenerateBackupPoms=false

  git add .
  git commit -m "Abrindo branch $BRANCH_NAME com versão ${VERSION}-SNAPSHOT"
  git push -u origin "$BRANCH_NAME"
}

# Atualiza develop para nova major
update_develop_for_major() {
  local NEXT_MAJOR=$1
  echo "==> Atualizando develop para ${NEXT_MAJOR}.0.0-SNAPSHOT"
  mvn versions:set -DnewVersion="${NEXT_MAJOR}.0.0-SNAPSHOT" -DgenerateBackupPoms=false
  git add .
  git commit -m "Atualizando develop para ${NEXT_MAJOR}.0.0-SNAPSHOT após major"
  git push origin develop
}

# Fluxo por tipo
case "$TYPE" in
  major)
    NEXT_MAJOR=$((CUR_MAJOR + 1))
    RELEASE_MINOR=$((CUR_MINOR + 1))
    RELEASE_VERSION="${CUR_MAJOR}.${RELEASE_MINOR}.0" # próxima release minor
    FIX_VERSION="${CUR_MAJOR}.${CUR_MINOR}.$((CUR_FIX + 1))" # fix da versão atual

    # Atualiza develop para próxima major
    update_develop_for_major "$NEXT_MAJOR"

    # Cria branch release/<próxima minor>
    create_branch_with_snapshot "release/$RELEASE_VERSION" "$RELEASE_VERSION"

    # Cria branch fix/<versão atual +1>
    git checkout develop
    create_branch_with_snapshot "fix/$FIX_VERSION" "$FIX_VERSION"
    ;;
  minor)
    RELEASE_VERSION="$NEW_BASE_VERSION" # próxima release minor
    FIX_VERSION="${CUR_MAJOR}.${CUR_MINOR}.$((CUR_FIX + 1))" # fix da versão atual

    # fix/X.Y.Z nasce da release/X.Y.0 atual, não de develop
    ORIGINAL_BRANCH=$(git branch --show-current)
    create_branch_with_snapshot "release/$RELEASE_VERSION" "$RELEASE_VERSION"

    git checkout "$ORIGINAL_BRANCH"
    create_branch_with_snapshot "fix/$FIX_VERSION" "$FIX_VERSION"
    ;;
  fix)
    FIX_VERSION="$NEW_BASE_VERSION"
    create_branch_with_snapshot "fix/$FIX_VERSION" "$FIX_VERSION"
    ;;
esac

echo "==> Processo de release finalizado com sucesso"
