
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

import br.com.sawcunhaos.foundation.jdempotent.core.utils.TestIdempotentResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestIdempotentResource.class})
class IdempotentWithoutAspectITTest {

    @Autowired
    private TestIdempotentResource testIdempotentResource;

    @Test
    void given_context_then_run_with_non_aop_context() {
        assertEquals(testIdempotentResource.getClass(), TestIdempotentResource.class);
        assertFalse(AopUtils.isAopProxy(testIdempotentResource));
        assertFalse(AopUtils.isCglibProxy(testIdempotentResource));

        assertEquals(AopProxyUtils.ultimateTargetClass(testIdempotentResource), TestIdempotentResource.class);
        assertEquals(AopTestUtils.getTargetObject(testIdempotentResource).getClass(), TestIdempotentResource.class);
        assertEquals(AopTestUtils.getUltimateTargetObject(testIdempotentResource).getClass(), TestIdempotentResource.class);
    }
}
