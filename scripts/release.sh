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

echo "==> Fechando versão"
./scripts/close_version.sh

echo "==> Criando tag $BASE_VERSION"
git tag -a "$BASE_VERSION" -m "Release $BASE_VERSION"
git push origin "$BASE_VERSION"

echo "==> Realizando deploy"
./scripts/deploy.sh

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

    create_branch_with_snapshot "release/$RELEASE_VERSION" "$RELEASE_VERSION"

    git checkout develop
    create_branch_with_snapshot "fix/$FIX_VERSION" "$FIX_VERSION"
    ;;
  fix)
    FIX_VERSION="$NEW_BASE_VERSION"
    create_branch_with_snapshot "fix/$FIX_VERSION" "$FIX_VERSION"
    ;;
esac

echo "==> Processo de release finalizado com sucesso"
