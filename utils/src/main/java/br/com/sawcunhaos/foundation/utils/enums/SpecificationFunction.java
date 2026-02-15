
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

package br.com.sawcunhaos.foundation.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SpecificationFunction {

    DAY("DAY", null),
    MONTH("MONTH", null),
    YEAR("YEAR", null),
    DATE_PART_DAY("DATE_PART", "DAY"),
    DATE_PART_MONTH("DATE_PART", "MONTH"),
    DATE_PART_YEAR("DATE_PART", "YEAR");

    private final String function;
    private final String param;

}
