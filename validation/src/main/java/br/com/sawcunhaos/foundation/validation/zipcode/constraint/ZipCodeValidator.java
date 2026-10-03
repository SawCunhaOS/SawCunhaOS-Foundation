
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

package br.com.sawcunhaos.foundation.validation.zipcode.constraint;

import br.com.sawcunhaos.foundation.validation.api.ZipCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bound to {@link ZipCode} at runtime via the XML constraint mapping in
 * {@code META-INF/validation.xml} (the annotation's {@code validatedBy} is empty to avoid a
 * {@code validation-api} → {@code validation} module cycle; see that module's Javadoc).
 */
public class ZipCodeValidator implements ConstraintValidator<ZipCode, String> {

    private static final Pattern CEP_VALID_REGEX = Pattern.compile("^\\d{2}\\d{3}(-)?\\d{3}$");

    /**
     * Verifica o valor contra o formato de CEP: 8 dígitos, opcionalmente com hífen antes dos 3 últimos
     * ({@code 01001000} ou {@code 01001-000}). Apenas o formato é verificado, não se o CEP
     * existe.
     *
     * @param zipCode o valor a verificar
     * @param cxt não utilizado
     * @return {@code true} se o formato casar; {@code false} para {@code null}, vazio ou qualquer outro
     *     formato (um CEP {@code null} é, portanto, rejeitado, diferente da convenção usual do Bean Validation;
     *     um campo de CEP opcional não pode usar esta constraint como está)
     */
    public boolean isValid(String zipCode, ConstraintValidatorContext cxt) {
        if(StringUtils.isBlank(zipCode)) {
            return false;
        }
        // O padrão é ancorado com ^ e $, então find() se comporta como matches(); a única peculiaridade é
        // que o $ do Java também casa antes de um único terminador de linha final ("01001000\n" passa).
        Matcher matcher = CEP_VALID_REGEX.matcher(zipCode);
        return matcher.find();
    }
}
