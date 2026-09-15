
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

package br.com.sawcunhaos.foundation.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * SQL function names usable in a JPA Criteria {@code function(name, ...)} call
 * for date-comparison specifications.
 *
 * <p>Consumed by {@code jpa.SpecificationRepository#specificationEqual(String, SpecificationFunction, Object)}:
 * when {@link #getParam()} is {@code null}, it calls the single-argument form
 * {@code function(name)(field)} (e.g. {@code DAY(field)}); when non-null, it calls the
 * two-argument form {@code function(name)(param, field)} (e.g. {@code DATE_PART('DAY', field)}).
 *
 * @since 1.2.0
 */
@Getter
@RequiredArgsConstructor
public enum SpecificationFunction {

    /** Extracts the day-of-month via the single-argument SQL {@code DAY(field)}. */
    DAY("DAY", null),
    /** Extracts the month via the single-argument SQL {@code MONTH(field)}. */
    MONTH("MONTH", null),
    /** Extracts the year via the single-argument SQL {@code YEAR(field)}. */
    YEAR("YEAR", null),
    /** Extracts the day-of-month via the two-argument SQL {@code DATE_PART('DAY', field)}. */
    DATE_PART_DAY("DATE_PART", "DAY"),
    /** Extracts the month via the two-argument SQL {@code DATE_PART('MONTH', field)}. */
    DATE_PART_MONTH("DATE_PART", "MONTH"),
    /** Extracts the year via the two-argument SQL {@code DATE_PART('YEAR', field)}. */
    DATE_PART_YEAR("DATE_PART", "YEAR");

    private final String function;
    private final String param;

}
