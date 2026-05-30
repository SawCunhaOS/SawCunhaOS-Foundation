
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

package br.com.sawcunhaos.foundation.privacy.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReDoSGuardTest {

    @Test
    void rejectsNestedQuantifier() {
        assertThrows(PrivacyConfigException.class, () -> ReDoSGuard.validate("(a+)+", "rule"));
        assertThrows(PrivacyConfigException.class, () -> ReDoSGuard.validate("(a*)*", "rule"));
    }

    @Test
    void acceptsSafePatterns() {
        assertDoesNotThrow(() -> ReDoSGuard.validate("\\d{11}", "rule"));
        assertDoesNotThrow(() -> ReDoSGuard.validate("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+", "rule"));
    }
}
