
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
import java.util.concurrent.atomic.AtomicLong;

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
     * against {@code contains()} (the expiry check) for a sustained duration.
     *
     * <p><strong>Story 3.20 debt:</strong> the identity race above is nanosecond-scale (a
     * handful of instructions between the expiry check and the evict), while any wall-clock
     * TTL large enough to survive real thread contention (originally 1ms, now 100ms — see the
     * in-method comment) is orders of magnitude larger. No TTL value here can reliably
     * re-catch that specific race if it regressed; this test is kept as a concurrent-load
     * smoke test (no entry lost, and its response not corrupted, under sustained concurrent
     * refresh+read). The identity race itself is now prevented structurally — a single atomic
     * {@code computeIfPresent} path, no separate get+remove anywhere in
     * {@code AbstractIdempotentRepository} — not by this test's assertions. A deterministic,
     * scheduling-independent regression test for that specific race is still missing (tracked
     * in {@code deferred-work.md}).
     */
    @Test
    void given_setResponse_repeatedly_refreshes_the_ttl_while_contains_races_the_expiry_check_then_the_entry_is_never_incorrectly_evicted() throws Exception {
        // Generous refresh TTL (was 1ms — deferred-work, story 3.20): with 8 threads hammering
        // ConcurrentHashMap's per-key lock on the very same key, the gap between two
        // consecutive refreshes can itself exceed a couple of ms under real contention — a
        // legitimate expiry, not a lost update. At 1ms this failed on ~100% of runs (tens of
        // millions of false "observed absent" results per run), independent of whether the
        // atomic fix was in place. Verified empirically (locally, 24 cores) that 100ms is
        // stable for both the fixed implementation and a temporarily-reintroduced broken one.
        long refreshTtlMs = 100L;
        InMemoryIdempotentRepository repository = new InMemoryIdempotentRepository();
        IdempotencyKey key = new IdempotencyKey("ttl-refresh-race-key");
        IdempotentResponseWrapper response = new IdempotentResponseWrapper("alive");

        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicInteger observedAbsent = new AtomicInteger();
        AtomicLong readAttempts = new AtomicLong();
        int threadsPerRole = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threadsPerRole * 2);
        try {
            // Seed the entry so setResponse() (present-only) has something to refresh.
            repository.store(key, new IdempotentRequestWrapper("payload"), refreshTtlMs, TimeUnit.MILLISECONDS);

            // Multiple refresher threads, each re-setting the response in a tight loop, so the
            // entry keeps going transiently expired between writes — exactly the window the
            // old check-then-act eviction could race against this method's in-place mutation
            // of the shared wrapper. Several concurrent refreshers/readers (real parallelism,
            // not just interleaving on one core) make that overlap far more likely to occur
            // within the test's duration than a single reader/writer pair would.
            List<Future<?>> refreshers = new ArrayList<>();
            for (int i = 0; i < threadsPerRole; i++) {
                refreshers.add(pool.submit(() -> {
                    while (!stop.get()) {
                        repository.setResponse(key, null, response, refreshTtlMs, TimeUnit.MILLISECONDS);
                    }
                }));
            }
            // Let refreshers reach steady state before readers start counting. Thread-pool
            // worker creation is lazy and JIT/class-loading warm-up is otherwise unbounded —
            // without this, the seeded entry could expire for real before any refresher thread
            // actually runs, misreporting a legitimate cold-start expiry as a race failure.
            Thread.sleep(refreshTtlMs);

            List<Future<?>> readers = new ArrayList<>();
            for (int i = 0; i < threadsPerRole; i++) {
                readers.add(pool.submit(() -> {
                    while (!stop.get()) {
                        readAttempts.incrementAndGet();
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

            // Guards against a vacuous pass (observedAbsent == 0 because the readers barely
            // ran, not because the fix holds) on a starved/overloaded box.
            assertTrue(readAttempts.get() > 1000,
                    "test did not exercise enough concurrent reads to be a meaningful signal: " + readAttempts.get());
            assertEquals(0, observedAbsent.get(),
                    "a concurrent setResponse() refreshing the TTL must never be lost to a racing expiry check");
            assertEquals("alive", repository.getResponse(key).getResponse(),
                    "the response must survive concurrent refreshes unmodified");
        } finally {
            pool.shutdown();
            pool.awaitTermination(2, TimeUnit.SECONDS);
        }
    }
}
