
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
import br.com.sawcunhaos.foundation.jdempotent.core.model.Lease;
import br.com.sawcunhaos.foundation.jdempotent.redis.configuration.ScosJdempotentRedisProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Story 3.5:
 * <ul>
 *   <li>Task 3 / AC3 — real-thread concurrency against a real Redis (Testcontainers,
 *       already on this module's {@code pom.xml}) proving exactly one concurrent
 *       {@code tryAcquire} call for the same key wins the atomic {@code SET ... NX PX}.</li>
 *   <li>Task 4 / AC4 — the lease-expires-before-the-method-finishes scenario: documents
 *       the observable behaviour (a later caller CAN acquire a new lease once the short
 *       TTL elapses, even though the "original" call never released it) instead of
 *       pretending this story eliminates that race — {@code idempotency.in_progress}
 *       (Story 3.11) is the production detection mechanism for it.</li>
 * </ul>
 *
 * <p>Uses a plain {@link GenericContainer} (a static field, so the class starts one
 * Redis and reuses it for every test method) instead of the module's docker-compose
 * fixture: no sentinel/HTTP layer is needed to exercise the repository directly, and a
 * dynamically mapped port avoids the fixed {@code 6379}/{@code 26379} host ports the
 * compose file uses (those collide across separate test classes/runs).</p>
 */
@Testcontainers
class RedisIdempotentRepositoryTryAcquireITTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("bitnami/redis:latest")
            .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
            .withExposedPorts(6379);

    static LettuceConnectionFactory connectionFactory;
    static RedisIdempotentRepository repository;

    @BeforeAll
    static void setUp() {
        RedisStandaloneConfiguration redisConfiguration =
                new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(redisConfiguration);
        connectionFactory.afterPropertiesSet();

        RedisTemplate<String, IdempotentRequestResponseWrapper> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new PolymorphicRedisSerializer());
        redisTemplate.afterPropertiesSet();

        ScosJdempotentRedisProperties properties = new ScosJdempotentRedisProperties();
        properties.setExpirationTimeHour(2L);
        properties.setPersistReqRes(true);

        repository = new RedisIdempotentRepository(redisTemplate, properties);
    }

    @AfterAll
    static void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void given_concurrent_real_threads_with_same_key_when_tryAcquire_then_exactly_one_acquires() throws Exception {
        IdempotencyKey key = new IdempotencyKey("concurrency-" + UUID.randomUUID());
        int threadCount = 10;
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
        assertEquals(1, acquiredCount, "exactly one real concurrent caller must acquire the Redis lease");

        long inProgressCount = results.stream().filter(l -> !l.isAcquired() && !l.hasCachedResponse()).count();
        assertEquals(threadCount - 1, inProgressCount, "every other concurrent caller must be told the key is already in progress");
    }

    @Test
    void given_a_short_ttl_when_it_expires_before_the_protected_method_finishes_then_a_later_call_can_acquire_again() throws Exception {
        IdempotencyKey key = new IdempotencyKey("ttl-expiry-" + UUID.randomUUID());

        // "protected method" starts and acquires the lease with a TTL shorter than its own work.
        Lease originalCall = repository.tryAcquire(key, "hash", Duration.ofMillis(300));
        assertTrue(originalCall.isAcquired());

        // While it is still "running", a concurrent retry correctly sees it as in progress.
        Lease concurrentRetry = repository.tryAcquire(key, "hash", Duration.ofSeconds(30));
        assertFalse(concurrentRetry.isAcquired());
        assertFalse(concurrentRetry.hasCachedResponse());

        // The lease's TTL elapses in Redis before the original call would have finished/removed it.
        Thread.sleep(600);

        // Documented risk (Task 4): a later call now acquires a brand-new lease for the same
        // key and would run in parallel with the still-in-flight original call. This story does
        // not eliminate that window (out of scope); idempotency.in_progress (Story 3.11) is the
        // mechanism that detects it in production, and Lease/tryAcquire already expose the
        // acquired/not-acquired signal that metric needs.
        Lease afterExpiry = repository.tryAcquire(key, "hash", Duration.ofSeconds(30));
        assertTrue(afterExpiry.isAcquired(), "once the lease TTL elapses, a later call is allowed to acquire a new one");
    }
}
