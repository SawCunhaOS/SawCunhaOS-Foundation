
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

package br.com.sawcunhaos.foundation.utils.aspect;

import br.com.sawcunhaos.foundation.core.utils.StringFieldUtils;
import br.com.sawcunhaos.foundation.utils.annotation.normalizestrings.NormalizeStrings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
public class StringProcessingAspect {

    @Around("@annotation(br.com.sawcunhaos.foundation.utils.annotation.normalizestrings.NormalizeStrings)")
    public Object handleNormalizeStrings(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        NormalizeStrings annotation = method.getAnnotation(NormalizeStrings.class);

        Arrays.stream(joinPoint.getArgs()).forEach(object -> {
            StringFieldUtils.applyTransformation(object, annotation.function()::apply);
        });

        return joinPoint.proceed();
    }

}
