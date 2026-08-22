
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

package br.com.sawcunhaos.foundation.validation.taxidentifier.constraint;

import br.com.sawcunhaos.foundation.validation.api.CNPJ;
import br.com.sawcunhaos.foundation.validation.api.CPF;
import br.com.sawcunhaos.foundation.validation.api.TaxIdentifier;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard for the {@code validatedBy = {}} + {@code META-INF/validation.xml} wiring
 * (Story 1.9): {@code @CPF}/{@code @CNPJ}/{@code @TaxIdentifier} live in {@code validation-api}
 * with an empty {@code validatedBy}, and only this module's XML constraint mapping binds them to
 * their {@code ConstraintValidator}. Without a passing end-to-end test here, a typo in the FQNs of
 * {@code validation-constraint-mappings.xml} would silently leave these 3 annotations unenforced
 * (Bean Validation treats an unmapped constraint as "no validator", not as a startup error) -
 * {@code ZipCodeValidatorTest} only proves the mechanism for {@code @ZipCode}.
 */
class TaxIdentifierAnnotationsValidatorTest {

    private record TaxIdDTO(@CPF String cpf, @CNPJ String cnpj, @TaxIdentifier String taxIdentifier) {
    }

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validDocumentsProduceNoViolations() {
        TaxIdDTO dto = new TaxIdDTO("11144477735", "11222333000181", "11144477735");

        Set<ConstraintViolation<TaxIdDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void invalidCpfIsReportedWithItsOwnMessage() {
        TaxIdDTO dto = new TaxIdDTO("00000000000", "11222333000181", "11144477735");

        Set<ConstraintViolation<TaxIdDTO>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertEquals("SCOS-007", violations.iterator().next().getMessage());
    }

    @Test
    void invalidCnpjIsReportedWithItsOwnMessage() {
        TaxIdDTO dto = new TaxIdDTO("11144477735", "00000000000000", "11144477735");

        Set<ConstraintViolation<TaxIdDTO>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertEquals("SCOS-008", violations.iterator().next().getMessage());
    }

    @Test
    void invalidTaxIdentifierIsReportedWithItsOwnMessage() {
        TaxIdDTO dto = new TaxIdDTO("11144477735", "11222333000181", "not-a-document");

        Set<ConstraintViolation<TaxIdDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertEquals("SCOS-006", violations.iterator().next().getMessage());
    }
}
