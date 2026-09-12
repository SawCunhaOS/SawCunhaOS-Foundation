
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

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Story 3.20 (AC #5): {@code @JdempotentResource} is a method-level annotation. Overriding a
 * method that carries it without repeating the annotation on the override means the override
 * does not carry it either -- method annotations are not carried across an override the way
 * {@code @Inherited} class-level annotations are. This subclass overrides {@code
 * idempotentMethod()} without repeating {@code @JdempotentResource}, so
 * {@code IdempotentAspectTest} can prove the aspect never activates for it (the real method body
 * runs every call, with no idempotency short-circuit).
 */
@Component
public class TestIdempotentResourceSubclass extends TestIdempotentResource {

    private final AtomicInteger overriddenMethodInvocationCount = new AtomicInteger();

    @Override
    public void idempotentMethod(IdempotentTestPayload testObject) {
        overriddenMethodInvocationCount.incrementAndGet();
    }

    public int getOverriddenMethodInvocationCount() {
        return overriddenMethodInvocationCount.get();
    }
}
