
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
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Story 3.9: same shape as {@link TestAopContext}, plus
 * {@code @EnableTransactionManagement} and a {@link RollbackTrackingTransactionManager}
 * bean, so a real Spring proxy is built with both the {@code IdempotentAspect}
 * advisor and Spring's {@code TransactionInterceptor} advisor in the chain —
 * needed to observe their relative ordering ({@code @Order} on
 * {@code IdempotentAspect}) around a {@code @Transactional} method.
 */
@Configuration
@EnableAspectJAutoProxy
@EnableTransactionManagement
@ComponentScan(basePackages = {"br.com.sawcunhaos.foundation.jdempotent.core"})
class TestAopTransactionalContext {

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

    @Bean
    RollbackTrackingTransactionManager transactionManager() {
        return new RollbackTrackingTransactionManager();
    }
}
