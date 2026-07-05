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

FILE="${1:?Uso: validate-pr-template.sh <arquivo-com-body-da-pr>}"

if [ ! -f "$FILE" ]; then
  echo "❌ Arquivo não encontrado: $FILE"
  exit 1
fi

# Normaliza CRLF (body pode vir de cliente Windows)
BODY="$(tr -d '\r' < "$FILE")"

# Body nulo/vazio/só espaços
if [ -z "$(printf '%s' "$BODY" | tr -d '[:space:]')" ]; then
  echo "❌ PR sem descrição"
  exit 1
fi

TEXT_SECTIONS=("## Descrição" "## Breaking Changes" "## Referências")
CHECK_SECTIONS=("## Tipo de Mudança" "## Módulo(s) Afetado(s)" "## Checklist")

errors=()

# Heading existe como linha exata?
section_exists() {
  printf '%s\n' "$BODY" | grep -qxF "$1"
}

# Linhas entre a heading $1 e a próxima heading "## "
section_lines() {
  printf '%s\n' "$BODY" | awk -v h="$1" '
    $0 == h { grab = 1; next }
    grab && /^## / { exit }
    grab { print }
  '
}

# Tem ao menos uma linha não-vazia e não-comentário?
section_has_content() {
  local content
  content="$(section_lines "$1" | grep -vE '^[[:space:]]*$' | grep -vE '^[[:space:]]*<!--.*-->[[:space:]]*$' || true)"
  [ -n "$content" ]
}

# Tem ao menos um checkbox marcado (- [x] / - [X])?
section_has_checked_box() {
  section_lines "$1" | grep -qiE '^[[:space:]]*- \[x\]'
}

# Seções de texto livre: existem + têm conteúdo
for h in "${TEXT_SECTIONS[@]}"; do
  if ! section_exists "$h"; then
    errors+=("Seção ausente: $h")
  elif ! section_has_content "$h"; then
    errors+=("Seção sem conteúdo (preencha abaixo da heading): $h")
  fi
done

# Seções de checkbox: existem + têm ao menos um [x]
for h in "${CHECK_SECTIONS[@]}"; do
  if ! section_exists "$h"; then
    errors+=("Seção ausente: $h")
  elif ! section_has_checked_box "$h"; then
    errors+=("Nenhum checkbox marcado em: $h")
  fi
done

if [ "${#errors[@]}" -gt 0 ]; then
  echo "❌ Validação do template de PR falhou:"
  for e in "${errors[@]}"; do
    echo "  - $e"
  done
  exit 1
fi

echo "✅ Template de PR válido"
