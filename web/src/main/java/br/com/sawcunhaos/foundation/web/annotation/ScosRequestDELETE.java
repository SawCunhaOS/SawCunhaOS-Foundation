
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

    @AliasFor(annotation = RequestMapping.class, attribute = "value")
    String[] uri();

    @AliasFor(annotation = ResponseStatus.class, attribute = "code")
    HttpStatus httpCode();

    @AliasFor(annotation = CacheEvict.class, attribute = "value")
    String[] nameCache() default "DISABLE";

    @AliasFor(annotation = CacheEvict.class, attribute = "condition")
    String condition() default "false";

}
