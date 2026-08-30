
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

package br.com.sawcunhaos.foundation.jdempotent.redis.repository;

import br.com.sawcunhaos.foundation.cache.PolymorphicRedisSerializer;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.Lease;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Story 3.7: coverage for the circuit-breaker-backed fail-open this story adds around Redis
 * operations in {@link RedisIdempotentRepository} — before this story there was no test at all
 * for Redis unavailability in this module (Dev Notes).
 *
 * <ul>
 *   <li>AC #1/#2: a business call must never block on a slow/unavailable Redis, and repeated
 *       failures must trip the breaker so later calls short-circuit instead of each paying the
 *       full {@code spring.data.redis.timeout} again.</li>
 *   <li>AC #3: split-brain — the lock is acquired for real, Redis then dies, {@code setResponse}
 *       fails open; the accepted, documented cost is an uncached response and a re-executed
 *       retry, never a blocked/failed business request.</li>
 *   <li>AC #4: the {@code OPEN -> HALF_OPEN} window — two concurrent same-key calls must not
 *       BOTH silently fail open without ever testing real Redis (the concrete bad scenario the
 *       AC names); {@code permittedNumberOfCallsInHalfOpenState(1)} guarantees exactly one of the
 *       two is the real trial call.</li>
 * </ul>
 *
 * <p>Each test gets its own container (instance {@code @Container} field, not static): AC #2/#3
 * pause the container mid-test, which must never leak into another test.</p>
 */
@Testcontainers
class RedisIdempotentRepositoryFailOpenITTest {

    private static final Duration TASK_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(2);

    @Container
    GenericContainer<?> redis = new GenericContainer<>("bitnami/redis:latest")
            .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private RedisIdempotentRepository repository;
    private CircuitBreaker circuitBreaker;
    private boolean paused;

    @BeforeEach
    void setUp() {
        // Story 3.7 AC #1: a short, explicit commandTimeout (not Spring Data Redis's 60s
        // default) so this test is fast AND so resolveSlowCallThreshold() picks up this exact
        // value for slow-call-duration-threshold — the mechanism under test is that the breaker
        // treats a call as "slow" precisely when it exceeds this configured Redis timeout.
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(COMMAND_TIMEOUT)
                .build();
        connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379)), clientConfiguration);
        connectionFactory.afterPropertiesSet();
        repository = RedisIdempotentRepositoryTopologyITTest.repositoryUsing(connectionFactory);
        circuitBreaker = (CircuitBreaker) ReflectionTestUtils.getField(repository, "circuitBreaker");

        // Establish the connection while Redis is genuinely up, so pausing the container below
        // exercises the command-level slow-call path (what slow-call-duration-threshold guards),
        // not a connection-establishment failure (a different failure mode, not this AC's focus).
        repository.contains(new IdempotencyKey("warmup-" + UUID.randomUUID()));
    }

    @AfterEach
    void tearDown() {
        // A paused container can hang Testcontainers' own teardown/removal — always resume it
        // first regardless of which assertion above may have failed. Guarded by the "paused"
        // flag (not just isRunning(), which docker reports true for a paused container too) so a
        // test that never paused Redis doesn't fail here trying to unpause an already-running one.
        if (paused) {
            unpause();
        }
        connectionFactory.destroy();
    }

    private void pause() {
        redis.getDockerClient().pauseContainerCmd(redis.getContainerId()).exec();
        paused = true;
    }

    private void unpause() {
        redis.getDockerClient().unpauseContainerCmd(redis.getContainerId()).exec();
        paused = false;
    }

    private RedisTemplate<String, IdempotentRequestResponseWrapper> rawTemplate() {
        RedisTemplate<String, IdempotentRequestResponseWrapper> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new PolymorphicRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Test
    void given_redis_paused_when_repeated_calls_then_breaker_opens_and_short_circuits_without_blocking_the_business_request() {
        pause();
        try {
            // AC #1/#2: every single call must fail open (never FAIL_CLOSED) — including while
            // the breaker is still CLOSED/accumulating data and each call pays up to the full
            // commandTimeout individually, exactly the "no short-circuit rápido" gap Task 1
            // identified as the real problem this story fixes.
            for (int i = 0; i < 5; i++) {
                Lease lease = repository.tryAcquire(new IdempotencyKey("warmup-fail-open-" + i), "hash", Duration.ofSeconds(30));
                assertTrue(lease.isAcquired(), "fail-open: a slow/unavailable Redis must never block the business request");
            }
            assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState(),
                    "after minimumNumberOfCalls(5) slow calls the breaker must OPEN — the 'mechanical' short-circuit this story adds");

            // AC #1: now that the breaker is OPEN, a further call must fail-open near
            // instantly — it must NOT pay another full commandTimeout the way the try/catch-only
            // fail-open that already existed before this story would have.
            long startNanos = System.nanoTime();
            Lease shortCircuited = repository.tryAcquire(new IdempotencyKey("short-circuited"), "hash", Duration.ofSeconds(30));
            long elapsedNanos = System.nanoTime() - startNanos;

            assertTrue(shortCircuited.isAcquired(), "fail-open must still hold once the breaker is OPEN");
            assertTrue(elapsedNanos < COMMAND_TIMEOUT.toNanos() / 2,
                    "an OPEN breaker must short-circuit fast (took " + Duration.ofNanos(elapsedNanos)
                            + "), not pay the full Redis command timeout again (" + COMMAND_TIMEOUT + ")");

            // contains()/store()/getResponse()/setResponse() must fail open too, not just
            // tryAcquire() — all five Redis-touching operations share the same breaker.
            IdempotencyKey key = new IdempotencyKey("other-ops-" + UUID.randomUUID());
            assertFalse(repository.contains(key));
            assertDoesNotThrow(() -> repository.store(key, null, 1L, TimeUnit.HOURS));
            assertDoesNotThrow(() -> repository.setResponse(key, null, new IdempotentResponseWrapper("r"), 1L, TimeUnit.HOURS));
            assertNull(repository.getResponse(key));
        } finally {
            unpause();
        }
    }

    @Test
    void given_redis_dies_after_the_lock_is_acquired_when_setResponse_fails_then_the_client_still_gets_a_success_and_the_response_is_not_cached() {
        // Story 3.7, AC #3: split-brain. Lock acquired for real (Redis up), Redis then dies
        // during processing, setResponse() fails — the business call must still succeed
        // (fail-open); the accepted, documented cost is that the response is not cached and a
        // retry re-executes (see RedisIdempotentRepository#setResponse Javadoc, NFR6).
        IdempotencyKey key = new IdempotencyKey("split-brain-" + UUID.randomUUID());

        Lease lease = repository.tryAcquire(key, "payload-hash", Duration.ofSeconds(30));
        assertTrue(lease.isAcquired(), "setup: the lock must be genuinely acquired against real Redis before it goes down");

        pause();
        IdempotentResponseWrapper response = new IdempotentResponseWrapper("business-result");
        assertDoesNotThrow(
                () -> repository.setResponse(key, null, response, 1L, TimeUnit.HOURS),
                "fail-open: setResponse() failing while Redis is down must never propagate to the caller / block the business response");
        unpause();

        // The accepted risk, made observable: read the key directly against real (now
        // unresponsive-breaker-bypassed) Redis — it must still be just tryAcquire's placeholder,
        // with no response ever stored.
        ValueOperations<String, IdempotentRequestResponseWrapper> rawOps = rawTemplate().opsForValue();
        IdempotentRequestResponseWrapper stored = rawOps.get(key.getKeyValue());
        assertTrue(stored != null, "the original tryAcquire placeholder must still be present under the key");
        assertNull(stored.getResponse(), "documented accepted risk (AC #3): the response must NOT be cached when Redis dies mid-processing");

        // And: "retry subsequente reexecuta" — force the breaker back to CLOSED (deterministic,
        // no need to wait for waitDurationInOpenState) so this next call genuinely re-checks real
        // Redis rather than fail-opening on breaker state alone; with no cached response, a retry
        // with the same payload sees plain in-progress, never a cached-response replay.
        circuitBreaker.transitionToClosedState();
        Lease retry = repository.tryAcquire(key, "payload-hash", Duration.ofSeconds(30));
        assertFalse(retry.isAcquired(), "the original lock is still held under its TTL");
        assertFalse(retry.hasCachedResponse(), "no response was ever cached, so a retry must re-execute the protected method, not replay a cached result");
    }

    @Test
    void given_the_breaker_transitions_open_to_half_open_when_two_concurrent_same_key_calls_race_then_not_both_fall_into_fail_open() throws Exception {
        // Story 3.7, AC #4. Redis stays reachable for this whole test — only the breaker's
        // internal state is forced, exactly as resilience4j intends for tests
        // (transitionToOpenState()/transitionToHalfOpenState() instead of waiting the real
        // waitDurationInOpenState).
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();

        // circuitBreaker.getMetrics().getNumberOfNotPermittedCalls() is NOT safe to read after
        // the race: with permittedNumberOfCallsInHalfOpenState(1), the one permitted trial call
        // succeeding immediately transitions HALF_OPEN -> CLOSED, and each resilience4j state
        // keeps its OWN metrics object — so a denial recorded while still HALF_OPEN is gone from
        // circuitBreaker.getMetrics() the instant the state has already moved on. The event
        // publisher has no such blind spot: events fire (and are counted here) at the moment they
        // happen, independent of whatever state the breaker is in by the time this test asserts.
        AtomicInteger notPermittedCount = new AtomicInteger();
        AtomicInteger successCount = new AtomicInteger();
        circuitBreaker.getEventPublisher().onCallNotPermitted(event -> notPermittedCount.incrementAndGet());
        circuitBreaker.getEventPublisher().onSuccess(event -> successCount.incrementAndGet());

        IdempotencyKey key = new IdempotencyKey("half-open-race-" + UUID.randomUUID());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        boolean completedNormally = false;
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Callable<Lease> call = () -> {
                ready.countDown();
                start.await();
                return repository.tryAcquire(key, "half-open-payload-hash", Duration.ofSeconds(30));
            };

            Future<Lease> first = pool.submit(call);
            Future<Lease> second = pool.submit(call);
            ready.await();
            start.countDown();

            Lease leaseA = first.get(TASK_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            Lease leaseB = second.get(TASK_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            completedNormally = true;

            // Both calls must return normally either way (fail-open contract): the business
            // request is never blocked regardless of which side of the race a caller lands on.
            assertTrue(leaseA.isAcquired());
            assertTrue(leaseB.isAcquired());

            // permittedNumberOfCallsInHalfOpenState(1): exactly ONE of the two concurrent
            // same-key calls must be denied by the breaker (CallNotPermittedException, caught by
            // the repository's existing fail-open catch) — the bad scenario AC #4 names
            // explicitly is BOTH calls silently falling into fail-open without either one ever
            // testing real Redis recovery.
            assertEquals(1, notPermittedCount.get(),
                    "exactly one of the two concurrent same-key calls must be denied in the HALF_OPEN window — "
                            + "if it were 0, the breaker never limited the trial calls; if it were 2, "
                            + "BOTH calls fell into fail-open without either testing real Redis (AC #4's named failure mode)");
            assertEquals(1, successCount.get(), "exactly one call must have been the real HALF_OPEN trial that actually reached Redis");

            // The other call must have genuinely reached real Redis: the key now exists there,
            // proving the HALF_OPEN trial call was real, not also short-circuited.
            ValueOperations<String, IdempotentRequestResponseWrapper> rawOps = rawTemplate().opsForValue();
            assertTrue(rawOps.get(key.getKeyValue()) != null, "the permitted HALF_OPEN trial call must have actually written the lock key to real Redis");
        } finally {
            if (completedNormally) {
                pool.shutdown();
            } else {
                pool.shutdownNow();
            }
        }
    }
}
