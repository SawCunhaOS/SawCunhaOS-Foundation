
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

package br.com.sawcunhaos.foundation.utils.validation.taxIdentifier;

import br.com.sawcunhaos.foundation.utils.validation.taxIdentifier.constraint.TaxIdentifierValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = TaxIdentifierValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TaxIdentifier {
    String message() default "SCOS-006";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
