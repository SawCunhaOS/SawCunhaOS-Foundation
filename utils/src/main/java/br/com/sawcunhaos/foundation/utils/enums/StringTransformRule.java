
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

import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public enum StringTransformRule {

    CAMEL_CASE {
        @Override
        public String apply(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }

            return org.apache.commons.lang3.StringUtils.uncapitalize(value.toLowerCase());
        }
    },
    UPPER_CASE {
        @Override
        public String apply(String value) {
            return value != null ? value.toUpperCase() : null;
        }
    },
    LOWER_CASE {
        @Override
        public String apply(String value) {
            return value != null ? value.toLowerCase() : null;
        }
    },
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

    public abstract String apply(String value);

}
