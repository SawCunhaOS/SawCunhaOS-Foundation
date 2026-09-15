
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

/**
 * Primitive value categories a foundation consumer may need to tag a field or
 * parameter with (e.g. to pick a formatter, validator, or comparison strategy).
 * No consumer in this reactor uses it yet.
 *
 * @since 1.2.0
 */
public enum ValueType {
    STRING, DOUBLE, NUMBER, INT, DATE, DATE_TIME, BOOLEAN;
}
