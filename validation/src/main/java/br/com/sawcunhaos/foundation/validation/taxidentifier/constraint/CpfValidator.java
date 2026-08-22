
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

import br.com.caelum.stella.validation.CPFValidator;
import br.com.sawcunhaos.foundation.validation.api.CPF;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Bound to {@link CPF} at runtime via the XML constraint mapping in
 * {@code META-INF/validation.xml} (the annotation's {@code validatedBy} is empty to avoid a
 * {@code validation-api} → {@code validation} module cycle; see that module's Javadoc).
 *
 * <p>{@code @Aspect}/{@code @Component} were dropped on the move from {@code utils}: Jakarta Bean
 * Validation instantiates {@code ConstraintValidator}s by reflection (no-arg constructor), not via
 * Spring DI, so those annotations were dead weight that would have forced this module to depend on
 * Spring/AspectJ. {@code ZipCodeValidator} never carried them and works identically.</p>
 */
public class CpfValidator implements ConstraintValidator<CPF, String> {

    private final CPFValidator cpfValidator = new CPFValidator();

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext constraintValidatorContext) {
        return cpfValidator.invalidMessagesFor(cpf).isEmpty();
    }

}
