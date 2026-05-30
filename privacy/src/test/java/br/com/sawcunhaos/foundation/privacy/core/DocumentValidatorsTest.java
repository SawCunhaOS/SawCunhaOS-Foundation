
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

package br.com.sawcunhaos.foundation.privacy.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentValidatorsTest {

    @Test
    void validatesLuhn() {
        assertTrue(DocumentValidators.isLuhnValid("4111 1111 1111 1111"));
        assertFalse(DocumentValidators.isLuhnValid("4111 1111 1111 1112"));
    }

    @Test
    void validatesCpf() {
        assertTrue(DocumentValidators.isCpfValid("529.982.247-25"));
        assertFalse(DocumentValidators.isCpfValid("111.111.111-11"));
        assertFalse(DocumentValidators.isCpfValid("123.456.789-00"));
    }

    @Test
    void validatesCnpj() {
        assertTrue(DocumentValidators.isCnpjValid("11.222.333/0001-81"));
        assertFalse(DocumentValidators.isCnpjValid("11.222.333/0001-00"));
    }

    @Test
    void passesDelegatesByName() {
        assertTrue(DocumentValidators.passes(null, "anything"));
        assertTrue(DocumentValidators.passes("cpf", "529.982.247-25"));
        assertFalse(DocumentValidators.passes("cpf", "111.111.111-11"));
    }
}
