
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

package br.com.sawcunhaos.foundation.jdempotent.core.datasource;

import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentResponseWrapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Story 3.15 (AC #1): {@code store()}/{@code setResponse()} on the in-memory repository
 * used to ignore their {@code ttl}/{@code timeUnit} parameters entirely — entries never
 * expired. These tests pin down that a short, positive TTL is now actually honored by
 * {@code contains()}/{@code getResponse()}, and that the pre-existing "no TTL configured"
 * behavior (ttl {@code <=0}/{@code null}, or a {@code null} time unit, the
 * {@code @JdempotentResource} default) still never expires.
 *
 * <p>TTL/sleep margins here are deliberately generous (200ms TTL, 500ms sleep) to stay
 * robust under CI scheduling jitter — see the Story 3.15 code review's flakiness finding.
 */
class InMemoryIdempotentRepositoryTtlTest {

    @Test
    void given_store_with_short_ttl_when_ttl_elapses_then_contains_and_getResponse_treat_entry_as_absent() throws InterruptedException {
        InMemoryIdempotentRepository repository = new InMemoryIdempotentRepository();
        IdempotencyKey key = new IdempotencyKey("store-short-ttl-key");

        repository.store(key, new IdempotentRequestWrapper("payload"), 200L, TimeUnit.MILLISECONDS);
        assertTrue(repository.contains(key), "entry must be present before the TTL elapses");

        Thread.sleep(500);

        assertFalse(repository.contains(key), "contains() must treat an expired entry as absent");
        assertNull(repository.getResponse(key), "getResponse() must treat an expired entry as absent");
    }

    @Test
    void given_setResponse_with_short_ttl_when_ttl_elapses_then_contains_and_getResponse_treat_entry_as_absent() throws InterruptedException {
        InMemoryIdempotentRepository repository = new InMemoryIdempotentRepository();
        IdempotencyKey key = new IdempotencyKey("setResponse-short-ttl-key");

        // Mirrors the real IdempotentAspect flow: tryAcquire() first, then setResponse()
        // carries the actual TTL configured on @JdempotentResource.
        repository.tryAcquire(key, "hash", java.time.Duration.ofSeconds(30));
        repository.setResponse(key, null, new IdempotentResponseWrapper("done"), 200L, TimeUnit.MILLISECONDS);
        assertTrue(repository.contains(key), "entry must be present before the TTL elapses");
        assertEquals("done", repository.getResponse(key).getResponse());

        Thread.sleep(500);

        assertFalse(repository.contains(key), "contains() must treat an expired entry as absent");
        assertNull(repository.getResponse(key), "getResponse() must treat an expired entry as absent");
    }

    @Test
    void given_ttl_zero_when_time_passes_then_entry_never_expires() throws InterruptedException {
        // ttl=0 is the @JdempotentResource default ("no custom TTL configured") — must keep
        // behaving as "no expiration", the pre-existing behavior for this in-memory repository.
        InMemoryIdempotentRepository repository = new InMemoryIdempotentRepository();
        IdempotencyKey key = new IdempotencyKey("zero-ttl-key");

        repository.store(key, new IdempotentRequestWrapper("payload"), 0L, TimeUnit.HOURS);
        Thread.sleep(50);

        assertTrue(repository.contains(key), "a zero/no-TTL entry must never expire");
    }

    @Test
    void given_ttl_null_when_time_passes_then_entry_never_expires() throws InterruptedException {
        InMemoryIdempotentRepository repository = new InMemoryIdempotentRepository();
        IdempotencyKey key = new IdempotencyKey("null-ttl-key");

        repository.store(key, new IdempotentRequestWrapper("payload"), null, TimeUnit.HOURS);
        Thread.sleep(50);

        assertTrue(repository.contains(key), "a null-TTL entry must never expire");
    }

    @Test
    void given_ttl_negative_when_time_passes_then_entry_never_expires() throws InterruptedException {
        InMemoryIdempotentRepository repository = new InMemoryIdempotentRepository();
        IdempotencyKey key = new IdempotencyKey("negative-ttl-key");

        repository.store(key, new IdempotentRequestWrapper("payload"), -1L, TimeUnit.HOURS);
        Thread.sleep(50);

        assertTrue(repository.contains(key), "a negative-TTL entry must never expire (guards against ttl<=0 regressing to ttl<0)");
    }

    @Test
    void given_ttl_positive_but_timeUnit_null_when_stored_then_never_expires_without_throwing() {
        // Code review finding: computeExpiresAt(ttl, null) used to NPE on timeUnit.toMillis(ttl)
        // when ttl>0 but timeUnit==null. Unreachable via @JdempotentResource (its attribute is
        // never null), but reachable by any direct caller of the public store()/setResponse().
        InMemoryIdempotentRepository repository = new InMemoryIdempotentRepository();
        IdempotencyKey key = new IdempotencyKey("null-timeUnit-key");

        repository.store(key, new IdempotentRequestWrapper("payload"), 100L, null);

        assertTrue(repository.contains(key), "ttl>0 with a null timeUnit must fall back to never-expires, not throw");
    }

    /**
     * Story 3.15 code review (finding #1): {@code IdempotentRequestResponseWrapper} has no
     * {@code equals()}/{@code hashCode()} override, and {@code setResponse()} mutates the
     * existing wrapper object in place — so a check-then-act eviction (read, check
     * {@code isExpired()}, then remove/replace by identity) can race a concurrent
     * {@code setResponse()} that refreshes that very same object, and still "match" the
     * identity comparison after the refresh, wiping out the just-written update. This drives
     * {@code setResponse()} (refreshing the TTL far faster than it can elapse) concurrently
     * against {@code contains()} (the expiry check) for a sustained duration: under the fixed,
     * atomic ({@code computeIfPresent}/{@code compute}) implementation the entry must never be
     * observed absent, since the refresher never lets the real TTL lapse.
     */
    @Test
    void given_setResponse_repeatedly_refreshes_the_ttl_while_contains_races_the_expiry_check_then_the_entry_is_never_incorrectly_evicted() throws Exception {
        InMemoryIdempotentRepository repository = new InMemoryIdempotentRepository();
        IdempotencyKey key = new IdempotencyKey("ttl-refresh-race-key");
        IdempotentResponseWrapper response = new IdempotentResponseWrapper("alive");

        // Seed the entry so setResponse() (present-only) has something to refresh.
        repository.store(key, new IdempotentRequestWrapper("payload"), 1L, TimeUnit.MILLISECONDS);

        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicInteger observedAbsent = new AtomicInteger();
        int threadsPerRole = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threadsPerRole * 2);
        try {
            // Multiple refresher threads, each re-setting the response with a deliberately tiny
            // (1ms) TTL in a tight loop. A tiny TTL (rather than a generous one) is intentional
            // here, unlike the fixed-deadline tests above: it lets the entry go transiently
            // expired between writes, which is exactly the window the old check-then-act
            // eviction could race against this method's in-place mutation of the shared
            // wrapper. Several concurrent refreshers/readers (real parallelism, not just
            // interleaving on one core) make that exact overlap far more likely to actually
            // occur within the test's duration than a single reader/writer pair would.
            List<Future<?>> refreshers = new ArrayList<>();
            for (int i = 0; i < threadsPerRole; i++) {
                refreshers.add(pool.submit(() -> {
                    while (!stop.get()) {
                        repository.setResponse(key, null, response, 1L, TimeUnit.MILLISECONDS);
                    }
                }));
            }
            List<Future<?>> readers = new ArrayList<>();
            for (int i = 0; i < threadsPerRole; i++) {
                readers.add(pool.submit(() -> {
                    while (!stop.get()) {
                        if (!repository.contains(key)) {
                            observedAbsent.incrementAndGet();
                        }
                    }
                }));
            }

            Thread.sleep(3000);
            stop.set(true);
            for (Future<?> f : refreshers) {
                f.get(2, TimeUnit.SECONDS);
            }
            for (Future<?> f : readers) {
                f.get(2, TimeUnit.SECONDS);
            }

            assertEquals(0, observedAbsent.get(),
                    "a concurrent setResponse() refreshing the TTL must never be lost to a racing expiry check");
        } finally {
            pool.shutdown();
            pool.awaitTermination(2, TimeUnit.SECONDS);
        }
    }
}
