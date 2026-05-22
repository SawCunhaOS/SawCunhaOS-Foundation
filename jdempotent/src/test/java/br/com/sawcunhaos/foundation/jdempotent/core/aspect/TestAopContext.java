
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

package br.com.sawcunhaos.foundation.jdempotent.core.aspect;

import br.com.sawcunhaos.foundation.jdempotent.core.datasource.InMemoryIdempotentRepository;
import br.com.sawcunhaos.foundation.jdempotent.core.generator.DefaultKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {"br.com.sawcunhaos.foundation.jdempotent.core"})
class TestAopContext {

    @Bean
    IdempotentAspect idempotentAspect(InMemoryIdempotentRepository inMemoryIdempotentRepository, DefaultKeyGenerator defaultKeyGenerator) {
        return new IdempotentAspect(inMemoryIdempotentRepository, defaultKeyGenerator);
    }

    @Bean
    InMemoryIdempotentRepository inMemoryIdempotentRepository() {
        return new InMemoryIdempotentRepository();
    }

    @Bean
    DefaultKeyGenerator defaultKeyGenerator() {
        return new DefaultKeyGenerator();
    }

}
