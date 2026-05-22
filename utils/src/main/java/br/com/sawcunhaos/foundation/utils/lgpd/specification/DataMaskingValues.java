
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

package br.com.sawcunhaos.foundation.utils.lgpd.specification;

import br.com.sawcunhaos.foundation.utils.lgpd.model.DataMask;

import java.util.Set;

public interface DataMaskingValues {

    default Set<DataMask> headersValue() {
        return Set.of();
    }
    default Set<DataMask> bodyValue() {
        return Set.of();
    }

}
