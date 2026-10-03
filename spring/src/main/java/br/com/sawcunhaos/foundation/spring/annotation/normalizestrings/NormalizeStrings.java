
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

package br.com.sawcunhaos.foundation.spring.annotation.normalizestrings;

import br.com.sawcunhaos.foundation.spring.enums.StringTransformRule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Normaliza os campos {@code String} dos argumentos de um método antes de ele executar.
 *
 * <p>Tratada por {@link br.com.sawcunhaos.foundation.spring.aspect.StringProcessingAspect}, portanto
 * só tem efeito em chamadas que atravessam um proxy do Spring (uma chamada vinda de outro método do mesmo
 * bean não é interceptada). Os argumentos são alterados no próprio objeto.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NormalizeStrings {

    /**
     * A transformação aplicada a cada campo {@code String}.
     *
     * @return a regra a aplicar; {@link StringTransformRule#UPPER_CASE} por padrão
     */
    StringTransformRule function() default StringTransformRule.UPPER_CASE;

}
