#!/usr/bin/env bash

set -e

TYPE="$1"

if [[ -z "$TYPE" ]]; then
  echo "Uso: ./bump-version.sh [major|minor|fix]"
  exit 1
fi

if [[ "$TYPE" != "major" && "$TYPE" != "minor" && "$TYPE" != "fix" ]]; then
  echo "Parâmetro inválido: $TYPE"
  echo "Use: major | minor | fix"
  exit 1
fi

# Obtém a versão atual do Maven
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

if [[ -z "$CURRENT_VERSION" ]]; then
  echo "Não foi possível obter a versão atual do projeto."
  exit 1
fi

echo "Versão atual: $CURRENT_VERSION"

# Validação: se já for SNAPSHOT, não faz nada
if [[ "$CURRENT_VERSION" == *"-SNAPSHOT" ]]; then
  echo "A versão já é SNAPSHOT. Nenhuma alteração foi realizada."
  exit 0
fi

# Remove possíveis sufixos (ex: 1.2.3-RC1)
BASE_VERSION=$(echo "$CURRENT_VERSION" | cut -d'-' -f1)

IFS='.' read -r MAJOR MINOR FIX <<< "$BASE_VERSION"

MAJOR=${MAJOR:-0}
MINOR=${MINOR:-0}
FIX=${FIX:-0}

case "$TYPE" in
  major)
    MAJOR=$((MAJOR + 1))
    MINOR=0
    FIX=0
    ;;
  minor)
    MINOR=$((MINOR + 1))
    FIX=0
    ;;
  fix)
    FIX=$((FIX + 1))
    ;;
esac

NEW_VERSION="${MAJOR}.${MINOR}.${FIX}-SNAPSHOT"

echo "Nova versão: $NEW_VERSION"

# Atualiza versão no pom.xml
mvn versions:set \
  -DnewVersion="$NEW_VERSION" \
  -DgenerateBackupPoms=false

echo "Versão atualizada com sucesso."
