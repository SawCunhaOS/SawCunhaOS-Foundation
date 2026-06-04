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

package br.com.sawcunhaos.foundation.audit.service;

import br.com.sawcunhaos.foundation.audit.specification.ScosAuditService;
import br.com.sawcunhaos.foundation.utils.annotation.audit.AuditAction;
import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import jakarta.persistence.Id;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@ConditionalOnProperty(prefix = "scos.audit", name = "enabled", havingValue = "true")
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ScosAuditReadAspect {

    private static final ExpressionParser SPEL = new SpelExpressionParser();

    private final ScosAuditService auditService;

    @Around("@annotation(auditable)")
    public Object aroundAuditableMethod(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        if (auditable.action() != AuditAction.READ) {
            return pjp.proceed();
        }

        Object result = pjp.proceed();

        try {
            emitReadEvent(pjp, auditable, result);
        } catch (Exception e) {
            log.warn("Failed to emit READ audit event for method {}", pjp.getSignature().getName(), e);
        }

        return result;
    }

    private void emitReadEvent(ProceedingJoinPoint pjp, Auditable auditable, Object result) {
        String entity = resolveEntity(auditable, result);
        String idEntity = resolveIdEntity(auditable, pjp, result);

        if (entity == null || idEntity == null) {
            log.debug("Could not resolve entity/idEntity for READ audit on {}", pjp.getSignature().getName());
            return;
        }

        auditService.recordRead(entity, idEntity);
    }

    private String resolveEntity(Auditable auditable, Object result) {
        if (!auditable.entity().isEmpty()) {
            return auditable.entity().toUpperCase();
        }
        if (result != null) {
            return result.getClass().getSimpleName().toUpperCase();
        }
        return null;
    }

    private String resolveIdEntity(Auditable auditable, ProceedingJoinPoint pjp, Object result) {
        if (!auditable.idEntitySpEL().isEmpty()) {
            try {
                MethodSignature sig = (MethodSignature) pjp.getSignature();
                Method method = sig.getMethod();
                Parameter[] params = method.getParameters();
                Object[] args = pjp.getArgs();

                StandardEvaluationContext ctx = new StandardEvaluationContext();
                for (int i = 0; i < params.length; i++) {
                    ctx.setVariable(params[i].getName(), args[i]);
                }
                Object value = SPEL.parseExpression(auditable.idEntitySpEL()).getValue(ctx);
                return value != null ? value.toString() : null;
            } catch (Exception e) {
                log.debug("SpEL evaluation failed for idEntitySpEL={}", auditable.idEntitySpEL(), e);
            }
        }
        if (result != null) {
            Object unwrapped = unwrap(result);
            if (unwrapped != null) {
                return extractIdFromEntity(unwrapped);
            }
        }
        return null;
    }

    private Object unwrap(Object result) {
        if (result instanceof java.util.Optional<?> opt) {
            return opt.orElse(null);
        }
        return result;
    }

    private String extractIdFromEntity(Object entity) {
        Class<?> clazz = entity.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    return value != null ? value.toString() : null;
                } catch (IllegalAccessException e) {
                    log.debug("Cannot access @Id field {} on {}", field.getName(), clazz.getSimpleName());
                }
            }
        }
        return null;
    }

}
