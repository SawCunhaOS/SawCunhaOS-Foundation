
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
import br.com.sawcunhaos.foundation.jdempotent.api.KeySource;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TestIdempotentResource {

    private final AtomicInteger keepFailedInvocationCount = new AtomicInteger();
    private final AtomicInteger shortTtlInvocationCount = new AtomicInteger();

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

    // Story 3.13 (AC #1): keySource=HEADER_THEN_FIELDS + headerName wired through to the real
    // AOP-proxied flow, exercised by IdempotentAspectTest.
    @JdempotentResource(cachePrefix = "TestIdempotentResource", keySource = KeySource.HEADER_THEN_FIELDS, headerName = "Idempotency-Key")
    public void idempotentMethodWithHeaderKeySource(IdempotentTestPayload testObject) {
    }

    // Story 3.15 (Task 4, Story 3.19 review gap): a custom, short TTL exercised end-to-end
    // through the real AOP-proxied IdempotentAspect.execute() -> repository path, not just
    // against the repository in isolation.
    @JdempotentResource(cachePrefix = "TestIdempotentResource", ttl = 200, ttlTimeUnit = TimeUnit.MILLISECONDS)
    public void idempotentMethodWithShortTtl(IdempotentTestPayload testObject) {
        shortTtlInvocationCount.incrementAndGet();
    }

    public int getShortTtlInvocationCount() {
        return shortTtlInvocationCount.get();
    }
}
