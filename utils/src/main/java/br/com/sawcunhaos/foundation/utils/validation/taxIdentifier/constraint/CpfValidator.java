
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

package br.com.sawcunhaos.foundation.utils.validation.taxIdentifier.constraint;

import br.com.caelum.stella.validation.CPFValidator;
import br.com.sawcunhaos.foundation.utils.validation.taxIdentifier.CPF;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CpfValidator implements ConstraintValidator<CPF, String> {

    private final CPFValidator cpfValidator = new CPFValidator();

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext constraintValidatorContext) {
        return cpfValidator.invalidMessagesFor(cpf).isEmpty();
    }

}
