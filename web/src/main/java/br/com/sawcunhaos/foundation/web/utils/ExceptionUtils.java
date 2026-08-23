
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

package br.com.sawcunhaos.foundation.web.utils;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExceptionUtils {

    public static List<String> findValuesAnnotation(Annotation annotation){
        List<String> values = new ArrayList<>();
        if(annotation instanceof Max max){
            values.add(String.valueOf(max.value()));
        }
        if(annotation instanceof Min min){
            values.add(String.valueOf(min.value()));
        }
        if(annotation instanceof Size size){
            values.add(String.valueOf(size.min()));
            values.add(String.valueOf(size.max()));
        }
        return values;
    }

    public static List<Object> getArgsValidation(Object[] arguments) {

        return Arrays.stream(arguments)
                .map(object -> {
                    if(object instanceof DefaultMessageSourceResolvable defaultMessageSourceResolvable) {
                        return defaultMessageSourceResolvable.getDefaultMessage();
                    }
                    return object;
                })
                .toList();

    }
}
