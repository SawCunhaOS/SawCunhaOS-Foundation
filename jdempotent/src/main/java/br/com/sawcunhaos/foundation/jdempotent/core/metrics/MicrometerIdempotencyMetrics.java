
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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Story 3.11: registers the {@code idempotency.*} meters on a real Micrometer
 * {@link MeterRegistry}. Only ever constructed when Micrometer is on the classpath
 * and a {@code MeterRegistry} bean exists — see {@code ScosJdempotentMetricsConfiguration}.
 * Never referenced (and therefore never classloaded) when either is missing, which is
 * what lets a consumer without Micrometer stay on {@link NoOpIdempotencyMetrics}
 * without a {@code NoClassDefFoundError}.
 */
public class MicrometerIdempotencyMetrics implements IdempotencyMetrics {

    private final Counter acquiredCounter;
    private final Counter hitCounter;
    private final Counter inProgressCounter;
    private final Counter mismatchCounter;
    private final Counter backendErrorCounter;
    private final Counter degradedTransitionsCounter;
    /** Backing value for the {@code idempotency.degraded} gauge — 1 degraded, 0 normal. */
    private final AtomicInteger degradedState = new AtomicInteger(0);

    public MicrometerIdempotencyMetrics(MeterRegistry registry) {
        this.acquiredCounter = Counter.builder("idempotency.acquired")
                .description("Number of idempotency locks acquired")
                .register(registry);
        this.hitCounter = Counter.builder("idempotency.hit")
                .description("Number of idempotency requests served from a cached response")
                .register(registry);
        this.inProgressCounter = Counter.builder("idempotency.in_progress")
                .description("Number of idempotency requests rejected because the same key is already in progress")
                .register(registry);
        this.mismatchCounter = Counter.builder("idempotency.mismatch")
                .description("Number of payload mismatches detected under the same idempotency key")
                .register(registry);
        this.backendErrorCounter = Counter.builder("idempotency.backend_error")
                .description("Number of idempotency backend (Redis) operation failures that fell into the fail-open path")
                .register(registry);
        this.degradedTransitionsCounter = Counter.builder("idempotency.degraded.transitions")
                .description("Number of transitions between normal and degraded idempotency backend state")
                .register(registry);
        Gauge.builder("idempotency.degraded", degradedState, AtomicInteger::get)
                .description("Whether the idempotency backend is currently degraded (1) or normal (0)")
                .register(registry);
    }

    @Override
    public void acquired() {
        acquiredCounter.increment();
    }

    @Override
    public void hit() {
        hitCounter.increment();
    }

    @Override
    public void inProgress() {
        inProgressCounter.increment();
    }

    @Override
    public void mismatch() {
        mismatchCounter.increment();
    }

    @Override
    public void backendError() {
        backendErrorCounter.increment();
    }

    @Override
    public void degraded(boolean degraded) {
        degradedState.set(degraded ? 1 : 0);
    }

    @Override
    public void degradedTransition() {
        degradedTransitionsCounter.increment();
    }
}
