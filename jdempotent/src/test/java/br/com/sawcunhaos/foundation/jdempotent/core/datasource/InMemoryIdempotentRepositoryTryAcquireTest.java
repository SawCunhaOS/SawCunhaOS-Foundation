
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
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.Lease;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Story 3.5 (AC3): real-thread concurrency coverage for the atomic
 * {@code putIfAbsent}-based {@code tryAcquire} in {@link AbstractIdempotentRepository}.
 * Complements the Redis-backed concurrency test, which is the one required by the
 * story's Task 3; this one is fast/Docker-free and pins down the same guarantee at
 * the in-memory implementation.
 */
class InMemoryIdempotentRepositoryTryAcquireTest {

    @Test
    void given_concurrent_real_threads_with_same_key_when_tryAcquire_then_exactly_one_acquires() throws Exception {
        InMemoryIdempotentRepository repository = new InMemoryIdempotentRepository();
        IdempotencyKey key = new IdempotencyKey("concurrency-key");
        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Callable<Lease>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                ready.countDown();
                start.await();
                return repository.tryAcquire(key, "same-payload-hash", Duration.ofSeconds(30));
            });
        }

        List<Future<Lease>> futures = new ArrayList<>();
        for (Callable<Lease> task : tasks) {
            futures.add(pool.submit(task));
        }
        ready.await();
        start.countDown();

        List<Lease> results = new ArrayList<>();
        for (Future<Lease> future : futures) {
            results.add(future.get());
        }
        pool.shutdown();

        long acquiredCount = results.stream().filter(Lease::isAcquired).count();
        assertEquals(1, acquiredCount, "exactly one concurrent caller must acquire the lease");

        long inProgressCount = results.stream().filter(l -> !l.isAcquired() && !l.hasCachedResponse()).count();
        assertEquals(threadCount - 1, inProgressCount, "every other caller must see the key as in-progress, not proceed");
    }

    @Test
    void given_lock_already_acquired_when_tryAcquire_again_then_not_acquired_and_no_cached_response() {
        InMemoryIdempotentRepository repository = new InMemoryIdempotentRepository();
        IdempotencyKey key = new IdempotencyKey("single-key");

        Lease first = repository.tryAcquire(key, "hash", Duration.ofSeconds(30));
        assertTrue(first.isAcquired());

        Lease second = repository.tryAcquire(key, "hash", Duration.ofSeconds(30));
        assertFalse(second.isAcquired());
        assertFalse(second.hasCachedResponse());
    }

    @Test
    void given_a_finished_call_with_cached_response_when_tryAcquire_then_lease_exposes_it() {
        InMemoryIdempotentRepository repository = new InMemoryIdempotentRepository();
        IdempotencyKey key = new IdempotencyKey("finished-key");

        repository.tryAcquire(key, "hash", Duration.ofSeconds(30));
        repository.setResponse(key, null, new IdempotentResponseWrapper("done"), 0L, java.util.concurrent.TimeUnit.HOURS);

        Lease lease = repository.tryAcquire(key, "hash", Duration.ofSeconds(30));
        assertFalse(lease.isAcquired());
        assertTrue(lease.hasCachedResponse());
        assertEquals("done", lease.getExistingResponse().getResponse());
    }

    /**
     * Story 3.6, AC #1: same key, second call finished the first with a different
     * payload hash -> the second call must see a mismatch instead of replaying the
     * cached response of the first payload.
     */
    @Test
    void given_a_finished_call_when_a_different_payload_arrives_under_the_same_key_then_lease_is_a_mismatch() {
        InMemoryIdempotentRepository repository = new InMemoryIdempotentRepository();
        IdempotencyKey key = new IdempotencyKey("collision-key");

        repository.tryAcquire(key, "hash-A", Duration.ofSeconds(30));
        repository.setResponse(key, null, new IdempotentResponseWrapper("done"), 0L, java.util.concurrent.TimeUnit.HOURS);

        Lease lease = repository.tryAcquire(key, "hash-B", Duration.ofSeconds(30));
        assertFalse(lease.isAcquired());
        assertTrue(lease.isMismatch());
        assertEquals("hash-A", lease.getExistingPayloadHash());
    }

    /**
     * Story 3.6, AC #2: same key, different payload arrives while the first call is
     * still in progress (no cached response yet) -> mismatch takes precedence over
     * "already in progress".
     */
    @Test
    void given_a_call_still_in_progress_when_a_different_payload_arrives_under_the_same_key_then_lease_is_a_mismatch_not_in_progress() {
        InMemoryIdempotentRepository repository = new InMemoryIdempotentRepository();
        IdempotencyKey key = new IdempotencyKey("race-collision-key");

        repository.tryAcquire(key, "hash-A", Duration.ofSeconds(30));

        Lease lease = repository.tryAcquire(key, "hash-B", Duration.ofSeconds(30));
        assertFalse(lease.isAcquired());
        assertTrue(lease.isMismatch());
        assertFalse(lease.hasCachedResponse());
    }

    /**
     * Story 3.6, AC #2: real-thread coverage of the same race window as the test above
     * — the second call's mismatch must win even when it genuinely races the first
     * call's still-in-flight lease, not just when it is called sequentially after.
     */
    @Test
    void given_concurrent_real_threads_with_same_key_and_different_payloads_when_tryAcquire_then_the_loser_sees_a_mismatch() throws Exception {
        InMemoryIdempotentRepository repository = new InMemoryIdempotentRepository();
        IdempotencyKey key = new IdempotencyKey("race-collision-concurrency-key");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Lease>> futures = new ArrayList<>();
        boolean completedNormally = false;
        try {
            Callable<Lease> callA = () -> {
                ready.countDown();
                start.await();
                return repository.tryAcquire(key, "hash-A", Duration.ofSeconds(30));
            };
            Callable<Lease> callB = () -> {
                ready.countDown();
                start.await();
                return repository.tryAcquire(key, "hash-B", Duration.ofSeconds(30));
            };

            futures.add(pool.submit(callA));
            futures.add(pool.submit(callB));
            ready.await();
            start.countDown();

            List<Lease> results = new ArrayList<>();
            for (Future<Lease> future : futures) {
                results.add(future.get());
            }

            long acquiredCount = results.stream().filter(Lease::isAcquired).count();
            assertEquals(1, acquiredCount, "exactly one of the two racing payloads must acquire the lease");

            long mismatchCount = results.stream().filter(Lease::isMismatch).count();
            assertEquals(1, mismatchCount, "the losing call, with a different payload, must see a mismatch");
            completedNormally = true;
        } finally {
            // Same pattern as RedisIdempotentRepositoryTopologyITTest's concurrency battery:
            // shutdown() alone doesn't interrupt threads still blocked (e.g. a future.get()
            // that never returns because of a bug), so a non-happy path gets shutdownNow()
            // + cancel(true) instead of leaking them.
            if (completedNormally) {
                pool.shutdown();
            } else {
                pool.shutdownNow();
                futures.forEach(f -> f.cancel(true));
            }
        }
    }
}
