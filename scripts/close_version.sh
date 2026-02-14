#!/bin/bash

echo "Closing versioning process..."

mvn versions:set -DnewVersion=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | sed 's/-SNAPSHOT//')
mvn versions:commit

echo "Version updated to release version."
echo "New version: $(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)"
