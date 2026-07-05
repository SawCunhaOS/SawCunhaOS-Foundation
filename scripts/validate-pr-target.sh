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

set -euo pipefail

HEAD_REF="${HEAD_REF:?Erro: HEAD_REF não definido}"
BASE_REF="${BASE_REF:?Erro: BASE_REF não definido}"
REPO="${GITHUB_REPOSITORY:-}"

pass() {
  echo "✅ PR de '$HEAD_REF' para '$BASE_REF' permitida."
  exit 0
}

# $1 = lista de bases permitidas (string)
fail() {
  echo "❌ PR de '$HEAD_REF' para '$BASE_REF' não é permitida."
  echo "   Origem:  $HEAD_REF"
  echo "   Destino: $BASE_REF"
  echo "   Bases permitidas: $1"
  exit 1
}

# Existência remota de um branch (true/false). Falha de rede/rate limit => false.
branch_exists_remote() {
  local b="$1"
  if [ -z "$REPO" ]; then
    echo false
    return 0
  fi
  if gh api "repos/$REPO/branches/$b" --silent >/dev/null 2>&1; then
    echo true
  else
    echo false
  fi
}

case "$HEAD_REF" in
  feature/*)
    pass
    ;;

  fix/*)
    IFS='.' read -r MAJOR MINOR FIX <<< "${HEAD_REF#fix/}"
    next_fix="fix/${MAJOR}.${MINOR}.$((FIX + 1))"
    next_minor="release/${MAJOR}.$((MINOR + 1)).0"

    allowed=("$next_minor")
    if [ "$(branch_exists_remote "$next_fix")" = "true" ]; then
      allowed+=("$next_fix")
    fi

    for a in "${allowed[@]}"; do
      if [ "$BASE_REF" = "$a" ]; then pass; fi
    done
    fail "${allowed[*]}"
    ;;

  release/*)
    IFS='.' read -r MAJOR MINOR FIX <<< "${HEAD_REF#release/}"
    next_minor="release/${MAJOR}.$((MINOR + 1)).0"

    allowed=("develop" "$next_minor")
    for a in "${allowed[@]}"; do
      if [ "$BASE_REF" = "$a" ]; then pass; fi
    done
    fail "${allowed[*]}"
    ;;

  *)
    echo "✅ Branch '$HEAD_REF' não reconhecido — sem verificação de target."
    exit 0
    ;;
esac
