
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

package br.com.sawcunhaos.foundation.jdempotent.core.utils;

import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Story 3.9: {@code @Transactional} + {@code @JdempotentResource} on the same
 * method, used to prove {@code IdempotentAspect}'s {@code @Order} places it
 * outside Spring's transactional interceptor.
 */
@Component
public class TestTransactionalIdempotentResource {

    @Transactional
    @JdempotentResource(cachePrefix = "TestTransactionalIdempotentResource")
    public void idempotentMethodThrowingBusinessExceptionUnderTransaction(IdempotentTestPayload testObject) {
        throw new TestException();
    }

    @Transactional
    @JdempotentResource(cachePrefix = "TestTransactionalIdempotentResource")
    public String idempotentMethodReturningNormallyUnderTransaction(IdempotentTestPayload testObject) {
        return "ok";
    }
}
