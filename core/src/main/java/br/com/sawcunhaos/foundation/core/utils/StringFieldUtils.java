
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

package br.com.sawcunhaos.foundation.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringFieldUtils {

    // Structural note (Story 1.7, AC #1): the original signature took the Spring-coupled
    // StringTransformRule enum (at the time in br.com.sawcunhaos.foundation.utils.enums; moved to
    // br.com.sawcunhaos.foundation.spring.enums in Story 1.8, still Spring-coupled). core cannot
    // depend on spring or utils (both depend on core, and a cycle is unbuildable), so the parameter
    // is generalized to UnaryOperator<String>. Callers holding a StringTransformRule pass
    // `rule::apply` — same behavior, no cycle. See StringProcessingAspect (now in spring) for the
    // call-site update.
    public static void applyTransformation(Object object, UnaryOperator<String> transform) {
        if (object == null || transform == null) {
            return;
        }

        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (field.getType().equals(String.class)) {
                field.setAccessible(true);
                try {
                    String originalValue = (String) field.get(object);
                    if (originalValue != null) {
                        String transformedValue = transform.apply(originalValue);
                        field.set(object, transformedValue);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Erro ao acessar o campo: " + field.getName(), e);
                }
            }
        }
    }

    public static void applyUpperCase(Object object) {
        applyTransformation(object, String::toUpperCase);
    }

    public static void applyLowerCase(Object object) {
        applyTransformation(object, String::toLowerCase);
    }

    public static void applyCamelCase(Object object) {
        applyTransformation(object, StringFieldUtils::camelCase);
    }

    public static void applyCapitalize(Object object) {
        applyTransformation(object, StringFieldUtils::capitalize);
    }

    private static String camelCase(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return StringUtils.uncapitalize(value.toLowerCase());
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Arrays.stream(value.split("\\s+"))
                .map(word -> StringUtils.capitalize(word.toLowerCase()))
                .collect(Collectors.joining(" "));
    }

}
