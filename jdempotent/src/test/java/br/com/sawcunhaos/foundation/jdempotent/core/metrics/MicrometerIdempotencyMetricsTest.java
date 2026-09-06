
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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Story 3.11 (AC #1): confirms the exact meter names/types the AC requires are what actually
 * get registered and updated on a real {@link MeterRegistry} — the aspect/repository unit
 * tests only verify calls against a <i>mocked</i> {@link IdempotencyMetrics}, so this is the
 * one place the real Micrometer wiring is exercised end to end.
 */
class MicrometerIdempotencyMetricsTest {

    private MeterRegistry registry;
    private MicrometerIdempotencyMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new MicrometerIdempotencyMetrics(registry);
    }

    @Test
    void countersIncrementUnderTheNamesTheAcRequires() {
        metrics.acquired();
        metrics.acquired();
        metrics.hit();
        metrics.inProgress();
        metrics.mismatch();
        metrics.backendError();

        assertEquals(2.0, registry.get("idempotency.acquired").counter().count());
        assertEquals(1.0, registry.get("idempotency.hit").counter().count());
        assertEquals(1.0, registry.get("idempotency.in_progress").counter().count());
        assertEquals(1.0, registry.get("idempotency.mismatch").counter().count());
        assertEquals(1.0, registry.get("idempotency.backend_error").counter().count());
    }

    @Test
    void degradedGaugeReflectsCurrentValueAndTransitionsCounterOnlyCountsWhatTheCallerReports() {
        assertEquals(0.0, registry.get("idempotency.degraded").gauge().value(),
                "starts normal (0) before any degraded() call");

        metrics.degraded(true);
        metrics.degradedTransition();
        assertEquals(1.0, registry.get("idempotency.degraded").gauge().value());
        assertEquals(1.0, registry.get("idempotency.degraded.transitions").counter().count());

        metrics.degraded(false);
        metrics.degradedTransition();
        assertEquals(0.0, registry.get("idempotency.degraded").gauge().value());
        assertEquals(2.0, registry.get("idempotency.degraded.transitions").counter().count());
    }
}
