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

# Pré-condição: só publica SNAPSHOT. Releases vão pelo release.sh, que define
# RELEASE_DEPLOY=1 para liberar o deploy da versão de release já fechada.
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
if [[ "$VERSION" != *-SNAPSHOT && "${RELEASE_DEPLOY:-}" != "1" ]]; then
  echo "Erro: versão $VERSION não é SNAPSHOT. Use release.sh para publicar releases."
  exit 1
fi

mvn clean deploy -DskipTests -Prelease

echo "Deploy realizado com sucesso."
