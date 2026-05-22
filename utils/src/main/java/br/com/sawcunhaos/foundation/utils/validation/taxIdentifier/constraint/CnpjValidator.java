
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

import br.com.caelum.stella.validation.CNPJValidator;
import br.com.sawcunhaos.foundation.utils.validation.taxIdentifier.CNPJ;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CnpjValidator implements ConstraintValidator<CNPJ, String> {

    private final CNPJValidator cnpjValidator = new CNPJValidator();

    @Override
    public boolean isValid(String cnpj, ConstraintValidatorContext constraintValidatorContext) {
        return cnpjValidator.invalidMessagesFor(cnpj).isEmpty();
    }

}
