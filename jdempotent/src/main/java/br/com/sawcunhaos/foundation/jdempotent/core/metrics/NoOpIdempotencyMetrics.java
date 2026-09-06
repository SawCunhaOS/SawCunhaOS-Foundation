
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

package br.com.sawcunhaos.foundation.jdempotent.core.metrics;

/**
 * Story 3.11: default {@link IdempotencyMetrics} implementation, does nothing.
 * Registered as the default bean ({@code ScosJdempotentMetricsConfiguration}) and used
 * as the default field value on {@code IdempotentAspect}/{@code RedisIdempotentRepository}
 * so a consumer without Micrometer on the classpath pays no cost and sees no
 * missing-bean error.
 */
public class NoOpIdempotencyMetrics implements IdempotencyMetrics {

    @Override
    public void acquired() {
        // no-op
    }

    @Override
    public void hit() {
        // no-op
    }

    @Override
    public void inProgress() {
        // no-op
    }

    @Override
    public void mismatch() {
        // no-op
    }

    @Override
    public void backendError() {
        // no-op
    }

    @Override
    public void degraded(boolean degraded) {
        // no-op
    }

    @Override
    public void degradedTransition() {
        // no-op
    }
}
