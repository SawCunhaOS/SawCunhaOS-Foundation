
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

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Estereótipo para um serviço que pertence à camada de regras: um {@code @Component} simples com um
 * nome específico de domínio, para que os colaboradores de regras se distingam de serviços genéricos.
 *
 * <p>Não carrega ordenação (diferente de {@link ScosRule}) e não adiciona comportamento além do registro
 * como componente.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ScosRuleService {
}
