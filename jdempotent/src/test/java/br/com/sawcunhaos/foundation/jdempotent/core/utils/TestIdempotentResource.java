
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


import br.com.sawcunhaos.foundation.jdempotent.api.IdempotentFailurePolicy;
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentRequestPayload;
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentResource;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TestIdempotentResource {

    private final AtomicInteger keepFailedInvocationCount = new AtomicInteger();

    @JdempotentResource
    public void idempotentMethod(IdempotentTestPayload testObject) {
    }

    @JdempotentResource(cachePrefix = "TestIdempotentResource")
    public void idempotentMethodThrowingARuntimeException(IdempotentTestPayload testObject) {
        throw new TestException();
    }

    @JdempotentResource(cachePrefix = "TestIdempotentResource", onBusinessException = IdempotentFailurePolicy.KEEP_FAILED)
    public void idempotentMethodThrowingARuntimeExceptionKeepFailed(IdempotentTestPayload testObject) {
        keepFailedInvocationCount.incrementAndGet();
        throw new TestException();
    }

    public int getKeepFailedInvocationCount() {
        return keepFailedInvocationCount.get();
    }

    @JdempotentResource(cachePrefix = "TestIdempotentResource")
    public void idempotentMethodWithThreeParameter(@JdempotentRequestPayload IdempotentTestPayload testObject, IdempotentTestPayload anotherObject, IdempotentTestPayload anotherObject2) {
    }

    @JdempotentResource(cachePrefix = "TestIdempotentResource")
    public void idempotentMethodWithThreeParamaterAndMultipleJdempotentRequestPayloadAnnotation(IdempotentTestPayload testObject, @JdempotentRequestPayload IdempotentTestPayload anotherObject, @JdempotentRequestPayload Object anotherObject2) {
    }

    @JdempotentResource(cachePrefix = "TestIdempotentResource")
    public void idempotentMethodWithZeroParamater() {
    }

    @JdempotentResource(cachePrefix = "TestIdempotentResource")
    public void methodWithTwoParamater(IdempotentTestPayload testObject, IdempotentTestPayload anotherObject) {
    }

    @JdempotentResource
    public IdempotentTestPayload idempotentMethodReturnArg(IdempotentTestPayload testObject) {
        return testObject;
    }

    @JdempotentResource
    public String idempotencyKeyAsString(@JdempotentRequestPayload String idempotencyKey) {
        return idempotencyKey;
    }
}
