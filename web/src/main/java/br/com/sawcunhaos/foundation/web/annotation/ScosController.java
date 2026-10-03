
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

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Estereótipo de controller REST SCOS: {@code @RestController} com prefixo de caminho fixo
 * {@code /api} e {@code produces = application/json}. O prefixo {@code /api} é o mesmo que
 * {@link br.com.sawcunhaos.foundation.web.filter.ScosFilterProperties#getURI()} usa para decidir
 * quais requisições os filtros de log registram.
 *
 * <p>Erros lançados pelo controller são convertidos em RFC 9457 pelo
 * {@link br.com.sawcunhaos.foundation.web.ExceptionsHandler}.</p>
 *
 * @since 1.2.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RestController
@RequestMapping(
        value = "/api",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public @interface ScosController {
}
