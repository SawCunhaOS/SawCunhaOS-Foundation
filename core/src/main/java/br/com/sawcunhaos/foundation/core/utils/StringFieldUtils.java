
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

/**
 * Reflection-based helpers to transform every {@code String} field of an object
 * in place (e.g. normalizing all string fields of a DTO to upper case before
 * persisting it).
 *
 * @since 1.2.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringFieldUtils {

    /**
     * Applies {@code transform} to every declared {@code String} field of
     * {@code object}, mutating it in place. Non-{@code String} fields and
     * {@code null}-valued {@code String} fields are silently skipped. No-op if
     * either argument is {@code null}.
     *
     * <p>Structural note (Story 1.7, AC #1): the original signature took the
     * Spring-coupled {@code StringTransformRule} enum (at the time in
     * {@code br.com.sawcunhaos.foundation.utils.enums}; moved to
     * {@code br.com.sawcunhaos.foundation.spring.enums} in Story 1.8, still
     * Spring-coupled). {@code core} cannot depend on {@code spring} or
     * {@code utils} (both depend on {@code core}, and a cycle is unbuildable), so
     * the parameter is generalized to {@link UnaryOperator}{@code <String>}.
     * Callers holding a {@code StringTransformRule} pass {@code rule::apply} —
     * same behavior, no cycle. See {@code StringProcessingAspect} (now in
     * {@code spring}) for the call-site update.
     *
     * <p>Implementation notes: this uses {@link Field#setAccessible(boolean)} to
     * write private fields directly, bypassing any setter/validation the class
     * may define. A field read failure ({@link IllegalAccessException}) is
     * wrapped and rethrown as an unchecked {@link RuntimeException} — callers
     * cannot recover from it selectively.
     *
     * @param object    the object whose {@code String} fields are transformed, or {@code null}
     * @param transform the transformation to apply to each non-null {@code String} field, or {@code null}
     */
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

    /**
     * Upper-cases every {@code String} field of {@code object} in place. See
     * {@link #applyTransformation(Object, UnaryOperator)} for the contract.
     *
     * @param object the object whose {@code String} fields are upper-cased
     */
    public static void applyUpperCase(Object object) {
        applyTransformation(object, String::toUpperCase);
    }

    /**
     * Lower-cases every {@code String} field of {@code object} in place. See
     * {@link #applyTransformation(Object, UnaryOperator)} for the contract.
     *
     * @param object the object whose {@code String} fields are lower-cased
     */
    public static void applyLowerCase(Object object) {
        applyTransformation(object, String::toLowerCase);
    }

    /**
     * Camel-cases every {@code String} field of {@code object} in place (lower-cases
     * the value, then uncapitalizes it). See
     * {@link #applyTransformation(Object, UnaryOperator)} for the contract.
     *
     * @param object the object whose {@code String} fields are camel-cased
     */
    public static void applyCamelCase(Object object) {
        applyTransformation(object, StringFieldUtils::camelCase);
    }

    /**
     * Capitalizes every word of every {@code String} field of {@code object} in
     * place. See {@link #applyTransformation(Object, UnaryOperator)} for the
     * contract.
     *
     * @param object the object whose {@code String} fields are capitalized
     */
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
