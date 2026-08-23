
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

package br.com.sawcunhaos.foundation.web.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validates the JSON Pointer (RFC 6901) conversion performed by
 * {@link ScosFieldError#of(String, String)}.
 */
class ScosFieldErrorTest {

    @Test
    @DisplayName("simple field becomes #/<field>")
    void simpleFieldBecomesPointer() {
        ScosFieldError error = ScosFieldError.of("email", "deve ser um e-mail válido", "CODE");

        assertEquals("#/email", error.pointer());
        assertEquals("deve ser um e-mail válido", error.detail());
    }

    @Test
    @DisplayName("nested field becomes #/<path>/<segments>")
    void nestedFieldBecomesNestedPointer() {
        ScosFieldError error = ScosFieldError.of("address.street", "não deve estar em branco", "CODE");

        assertEquals("#/address/street", error.pointer());
        assertEquals("não deve estar em branco", error.detail());
    }
}
