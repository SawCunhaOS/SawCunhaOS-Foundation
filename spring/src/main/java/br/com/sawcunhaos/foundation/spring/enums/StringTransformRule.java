
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

package br.com.sawcunhaos.foundation.spring.enums;

import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Transformações de caixa/formato selecionáveis em {@link
 * br.com.sawcunhaos.foundation.spring.annotation.normalizestrings.NormalizeStrings#function()}.
 * Toda constante é null-safe: {@code null} entra, {@code null} sai.
 */
@Getter
public enum StringTransformRule {

    /**
     * Converte o valor para minúsculas e, em seguida, des-capitaliza. Como o valor já está em minúsculas,
     * o resultado é igual a {@link #LOWER_CASE}; os espaços não são removidos, então não é camelCase de verdade.
     */
    CAMEL_CASE {
        @Override
        public String apply(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }

            return org.apache.commons.lang3.StringUtils.uncapitalize(value.toLowerCase());
        }
    },

    /** Converte o valor inteiro para maiúsculas. */
    UPPER_CASE {
        @Override
        public String apply(String value) {
            return value != null ? value.toUpperCase() : null;
        }
    },

    /** Converte o valor inteiro para minúsculas. */
    LOWER_CASE {
        @Override
        public String apply(String value) {
            return value != null ? value.toLowerCase() : null;
        }
    },

    /** Converte o valor para minúsculas e capitaliza cada palavra separada por espaço ({@code "jOHN dOE"} → {@code "John Doe"}); sequências de espaços colapsam em um só. */
    CAPITALIZE {
        @Override
        public String apply(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            return Arrays.stream(value.split("\\s+"))
                    .map(word -> StringUtils.capitalize(word.toLowerCase()))
                    .collect(Collectors.joining(" "));
        }
    };

    /**
     * Aplica esta transformação.
     *
     * @param value o texto a transformar, pode ser {@code null}
     * @return o texto transformado, ou {@code null} se {@code value} for {@code null}
     */
    public abstract String apply(String value);

}
