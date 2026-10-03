
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

package br.com.sawcunhaos.foundation.spring.annotation.rules;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca uma classe como bean do Spring que é uma regra dentro de um conjunto ordenado (por exemplo, uma cadeia de
 * regras de validação ou de negócio injetada como {@code List}).
 *
 * <p>Meta-anotada com {@code @Component} (detectada pelo component scan) e {@code @Order}:
 * {@link #value()} é um alias de {@code @Order.value}, então um único {@code @ScosRule(10)} registra
 * o bean e define sua posição quando o Spring ordena coleções injetadas. Valores menores
 * executam primeiro; o padrão é {@link Ordered#LOWEST_PRECEDENCE} (por último). Use
 * {@link ScosRuleService} no lugar, para um bean sem ordenação.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@Order
public @interface ScosRule {

    /**
     * O valor de ordem, com alias para {@code @Order.value}.
     *
     * @return a posição desta regra; valores menores têm maior prioridade
     */
    @AliasFor(annotation = Order.class, attribute = "value")
    int value() default Ordered.LOWEST_PRECEDENCE;
}
