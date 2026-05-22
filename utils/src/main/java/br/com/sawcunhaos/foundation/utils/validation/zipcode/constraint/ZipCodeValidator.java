
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

package br.com.sawcunhaos.foundation.utils.validation.zipcode.constraint;

import br.com.sawcunhaos.foundation.utils.validation.zipcode.ZipCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZipCodeValidator implements ConstraintValidator<ZipCode, String> {

    private static final Pattern CEP_VALID_REGEX = Pattern.compile("^\\d{2}\\d{3}(-)?\\d{3}$");

    public boolean isValid(String zipCode, ConstraintValidatorContext cxt) {
        if(StringUtils.isBlank(zipCode)) {
            return false;
        }
        Matcher matcher = CEP_VALID_REGEX.matcher(zipCode);
        return matcher.find();
    }
}
