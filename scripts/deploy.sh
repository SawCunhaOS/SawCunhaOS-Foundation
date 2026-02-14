#!/usr/bin/env bash

set -e

mvn clean deploy -DskipTests -Prelease

echo "Deploy realizado com sucesso."
