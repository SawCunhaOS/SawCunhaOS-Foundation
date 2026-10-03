
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

package br.com.sawcunhaos.foundation.web.annotation;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mapeia um método de controller para atender requisições HTTP DELETE sob a convenção SCOS (meta-anotação sobre
 * {@link ScosRequestMapping}). Não impõe {@code consumes}.
 *
 * <p>Cache: aplica {@code @CacheEvict(allEntries = true)}: após o método, esvazia todas as entradas do(s) cache(s) em {@code nameCache}. Com o padrão {@code condition = "false"} a limpeza fica <b>desligada</b> até o chamador passar uma condição SpEL verdadeira (ex.: {@code condition = "true"}).</p>
 *
 * <p>Atributos: {@code uri} (caminho, obrigatório), {@code httpCode} (status de sucesso,
 * obrigatório) e {@code nameCache} (nome(s) do cache; o padrão {@code "DISABLE"} não é um cache real).</p>
 *
 * @since 1.2.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ScosRequestMapping(
        method = RequestMethod.DELETE
)
@CacheEvict(
        allEntries=true
)
public @interface ScosRequestDELETE {

    /** Caminho(s) da requisição (alias de {@code RequestMapping.value}); obrigatório. */
    @AliasFor(annotation = RequestMapping.class, attribute = "value")
    String[] uri();

    /** Status HTTP de sucesso devolvido (alias de {@code ResponseStatus.code}); obrigatório. */
    @AliasFor(annotation = ResponseStatus.class, attribute = "code")
    HttpStatus httpCode();

    /** Nome(s) do cache; o padrão {@code "DISABLE"} não é um cache real. */
    @AliasFor(annotation = CacheEvict.class, attribute = "value")
    String[] nameCache() default "DISABLE";

    /** Condição SpEL que habilita a operação de cache; o padrão {@code "false"} a mantém desligada. */
    @AliasFor(annotation = CacheEvict.class, attribute = "condition")
    String condition() default "false";

}
