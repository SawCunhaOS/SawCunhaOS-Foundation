
/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Foundation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.foundation.utils.specification;

import java.util.List;

public interface ScosPermission {
    String getPermission();
    String getDescriptionPtBr();
    String getDescriptionEng();
    String getEndPoint();
    String getModule();
    List<ScosFeature> getFeatures();
}
