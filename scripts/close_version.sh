#!/bin/bash


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

echo "Closing versioning process..."

mvn versions:set -DnewVersion=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | sed 's/-SNAPSHOT//')
mvn versions:commit

echo "Version updated to release version."
echo "New version: $(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)"
