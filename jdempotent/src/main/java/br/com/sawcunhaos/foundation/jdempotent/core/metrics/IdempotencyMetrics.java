
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
 * Story 3.11 (FR7): emission points for idempotency observability. One method per
 * event this module can produce, so an operator can detect production problems such
 * as a client generating a new idempotency key inside its own retry loop (that shows
 * up as an anomalous {@code idempotency.acquired}/{@code idempotency.hit} ratio) or a
 * lease expiring before the protected method finishes (visible as
 * {@code idempotency.in_progress}, Story 3.5 AC #4).
 *
 * <p>Two implementations ship with this module: {@link NoOpIdempotencyMetrics}
 * (registered as the default bean, zero cost, no dependency required) and
 * {@link MicrometerIdempotencyMetrics} (registered instead when Micrometer is on the
 * consumer's classpath). Consumers without Micrometer never pay for it and never see
 * a missing-bean error — see {@code ScosJdempotentMetricsConfiguration}.</p>
 *
 * <p>Ponytail (Dev Notes, Story 3.11): this is intentionally not a pluggable
 * "metrics provider" abstraction supporting multiple simultaneous backends — the AC
 * asks specifically for Micrometer-conditional with a no-op fallback, nothing else.</p>
 */
public interface IdempotencyMetrics {

    /**
     * {@code idempotency.acquired}: {@code tryAcquire} obtained the lock — this call
     * owns the key and will execute the protected method.
     */
    void acquired();

    /**
     * {@code idempotency.hit}: the key already had a cached response, served from
     * cache instead of re-running the protected method.
     */
    void hit();

    /**
     * {@code idempotency.in_progress}: {@code tryAcquire} found the key already held
     * by another in-flight call (409) — the production-detection signal for a lease
     * expiring before the protected method finishes (Story 3.5, AC #4).
     */
    void inProgress();

    /**
     * {@code idempotency.mismatch}: the stored payload hash under this key differs
     * from the current call's (422 {@code PAYLOAD_MISMATCH}, Story 3.6).
     */
    void mismatch();

    /**
     * {@code idempotency.backend_error}: an operation against the idempotency
     * backend (Redis) failed and fell into the fail-open path (Story 3.7).
     */
    void backendError();

    /**
     * {@code idempotency.degraded} (gauge, 0/1): whether the idempotency backend is
     * currently operating in degraded mode (e.g. circuit breaker open, Story 3.7).
     * Reflects the current state — call with the same value again is harmless.
     *
     * @param degraded {@code true} when degraded, {@code false} when back to normal
     */
    void degraded(boolean degraded);

    /**
     * {@code idempotency.degraded.transitions} (counter): incremented once per
     * transition of the {@link #degraded(boolean)} state (normal-to-degraded or
     * degraded-to-normal) — not once per check.
     */
    void degradedTransition();
}
