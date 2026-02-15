
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

package br.com.sawcunhaos.foundation.utils.utils;

import br.com.sawcunhaos.foundation.utils.enums.SpecificationFunction;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SpecificationRepository {

    public static <T> Specification<T> specificationBetween(final String field, final LocalDate startDate, final LocalDate endDate) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get(field), startDate, endDate);
    }

    public static <T> Specification<T> specificationGreaterThanOrEqualTo(final String field, final LocalDate date) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get(field), date);
    }

    public static <T> Specification<T> specificationLessThanOrEqualTo(final String field, final LocalDate date) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get(field), date);
    }

    public static <T, O> Specification<T> specificationEqual(final String field, final O value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(field), value);
    }

    public static <T, C> Specification<T> specificationEqual(
            final String field,
            final SpecificationFunction function,
            final C value
    ) {
        return (root, query, criteriaBuilder) -> {
            if(Objects.isNull(function.getParam())) {
                return criteriaBuilder.equal(criteriaBuilder.function(function.getFunction(), value.getClass(), root.get(field)), value);
            } else {
                return criteriaBuilder.equal(criteriaBuilder.function(function.getFunction(), value.getClass(), criteriaBuilder.literal(function.getParam()), root.get(field)), value);
            }
        };
    }

    public static <Y, T, O> Specification<T> specificationEqual(final O value, final String... fields) {
        return (root, query, criteriaBuilder) -> {
            Path<Y> path = getField(root, Arrays.stream(fields).toList());
            return criteriaBuilder.equal(path, value);
        };
    }

    private static <Y, T> Path<Y> getField(Root<T> root, List<String> fields) {
        Path<Y> path = root.get(fields.get(0));

        for(int index = 1; index < fields.size(); index++) {
            path = getFieldPath(path, fields.get(index));
        }

        return path;
    }

    private static <Y> Path<Y> getFieldPath(Path<Y> path, String field) {
        return path.get(field);
    }
}
