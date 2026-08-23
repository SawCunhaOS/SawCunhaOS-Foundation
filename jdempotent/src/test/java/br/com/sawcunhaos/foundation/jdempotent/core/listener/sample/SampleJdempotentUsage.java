
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

package br.com.sawcunhaos.foundation.jdempotent.core.listener.sample;

import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentId;
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentResource;

/**
 * Fixture for {@code JdempotentAnnotationCountListenerTest}: 2 {@code @JdempotentResource}
 * methods, 1 {@code @JdempotentId} field.
 */
public class SampleJdempotentUsage {

    @JdempotentId
    private String idempotencyId;

    @JdempotentResource
    public void firstResource() {
    }

    @JdempotentResource(cachePrefix = "second")
    public void secondResource() {
    }

    public void notAnnotated() {
    }

}
