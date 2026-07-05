
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

import br.com.sawcunhaos.foundation.utils.specification.ExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validates the RFC 9457 metadata carried by {@link ExceptionCode} and its
 * {@link ScosExceptionCode} implementation: {@code type} URI derivation (default
 * behaviour) and the per-category {@code title} overrides.
 */
class ScosExceptionCodeTest {

    /**
     * An {@link ExceptionCode} that does NOT override {@code getType}/{@code getTitle},
     * exercising the interface defaults.
     */
    private static final ExceptionCode CODE_WITHOUT_OVERRIDE = () -> "SCOS-999";

    @Test
    @DisplayName("default getType() derives the problem URI from the code")
    void defaultGetTypeDerivesUriFromCode() {
        assertEquals(
                URI.create("https://docs.sawcunhaos.com.br/problems/scos-999"),
                CODE_WITHOUT_OVERRIDE.getType()
        );
    }

    @Test
    @DisplayName("default getTitle() returns the generic \"Error\"")
    void defaultGetTitleReturnsGenericError() {
        assertEquals("Error", CODE_WITHOUT_OVERRIDE.getTitle());
    }

    @Test
    @DisplayName("ScosExceptionCode derives type URI from its code")
    void scosExceptionCodeDerivesType() {
        assertEquals(
                URI.create("https://docs.sawcunhaos.com.br/problems/scos-001"),
                ScosExceptionCode.ATTRIBUTE_NOT_VALID.getType()
        );
    }

    @Test
    @DisplayName("ScosExceptionCode overrides title for validation errors")
    void scosExceptionCodeOverridesValidationTitle() {
        assertEquals("Validation Error", ScosExceptionCode.ATTRIBUTE_NOT_VALID.getTitle());
    }

    @Test
    @DisplayName("ScosExceptionCode overrides title for access denied")
    void scosExceptionCodeOverridesAccessDeniedTitle() {
        assertEquals("Access Denied", ScosExceptionCode.ACCESS_DENIED.getTitle());
    }
}
