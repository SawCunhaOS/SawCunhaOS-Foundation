
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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Story 3.19 / AD-7: the Standalone leg of the topology coverage — the FULL concurrency battery
 * (10 real threads, same key, migrated verbatim from {@code RedisIdempotentRepositoryTryAcquireITTest},
 * Story 3.5), the TTL-expiry documentation test, and the 2 informative TPS tests (AD-9): one with
 * manual wiring (this class), one resolving the repository via Spring DI
 * ({@code RedisIdempotentRepositoryTpsSpringContextITTest}, its own file/context).
 *
 * <p>Sentinel/Cluster live in their own class ({@code RedisIdempotentRepositoryTopologySmokeITTest})
 * on purpose (review finding, Story 3.19 iteration 1): all 4 containers used to be
 * {@code static @Container} fields on one class, so a Sentinel/Cluster infra failure (e.g. the
 * Cluster wait timing out) prevented even the Standalone regression battery — the most critical
 * test in this story — from running at all, despite having no relation to Sentinel/Cluster.</p>
 *
 * <p>Every assertion goes only through {@code tryAcquire(...) -> Lease} (AD-2 inherited) —
 * never {@code contains()}/{@code store()}/{@code setResponse()} in isolation (R-003).</p>
 */
@Testcontainers
class RedisIdempotentRepositoryTopologyITTest {

    private static final Duration TASK_TIMEOUT = Duration.ofSeconds(30);

    @Container
    static GenericContainer<?> standaloneRedis = new GenericContainer<>("bitnami/redis:latest")
            .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
            .withExposedPorts(6379);

    static LettuceConnectionFactory standaloneConnectionFactory;
    static RedisIdempotentRepository standaloneRepository;

    @BeforeAll
    static void setUpAll() {
        RedisStandaloneConfiguration configuration =
                new RedisStandaloneConfiguration(standaloneRedis.getHost(), standaloneRedis.getMappedPort(6379));
        standaloneConnectionFactory = new LettuceConnectionFactory(configuration);
        standaloneConnectionFactory.afterPropertiesSet();
        standaloneRepository = repositoryUsing(standaloneConnectionFactory);
    }

    @AfterAll
    static void tearDownAll() {
        standaloneConnectionFactory.destroy();
    }

    static RedisIdempotentRepository repositoryUsing(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, IdempotentRequestResponseWrapper> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new PolymorphicRedisSerializer());
        redisTemplate.afterPropertiesSet();

        ScosJdempotentRedisProperties properties = new ScosJdempotentRedisProperties();
        properties.setExpirationTimeHour(2L);
        properties.setPersistReqRes(true);

        return new RedisIdempotentRepository(redisTemplate, properties);
    }

    @Test
    void given_concurrent_real_threads_with_same_key_when_tryAcquire_then_exactly_one_acquires_standalone() throws Exception {
        IdempotencyKey key = new IdempotencyKey("concurrency-" + UUID.randomUUID());
        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<Lease>> futures = new ArrayList<>();
        boolean completedNormally = false;
        try {
            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch start = new CountDownLatch(1);

            List<Callable<Lease>> tasks = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                tasks.add(() -> {
                    ready.countDown();
                    start.await();
                    return standaloneRepository.tryAcquire(key, "same-payload-hash", Duration.ofSeconds(30));
                });
            }

            for (Callable<Lease> task : tasks) {
                futures.add(pool.submit(task));
            }
            ready.await();
            start.countDown();

            List<Lease> results = new ArrayList<>();
            for (Future<Lease> future : futures) {
                results.add(future.get(TASK_TIMEOUT.toSeconds(), TimeUnit.SECONDS));
            }

            long acquiredCount = results.stream().filter(Lease::isAcquired).count();
            assertEquals(1, acquiredCount, "exactly one real concurrent caller must acquire the Redis lease");

            long inProgressCount = results.stream().filter(l -> !l.isAcquired() && !l.hasCachedResponse()).count();
            assertEquals(threadCount - 1, inProgressCount, "every other concurrent caller must be told the key is already in progress");
            completedNormally = true;
        } finally {
            // shutdown() alone doesn't interrupt threads still blocked on Redis I/O (e.g. a
            // future.get timeout) — they'd keep running against a container @AfterAll may have
            // already torn down. shutdownNow() + cancel(true) on the happy-path exception only.
            if (completedNormally) {
                pool.shutdown();
            } else {
                pool.shutdownNow();
                futures.forEach(f -> f.cancel(true));
            }
        }
    }

    @Test
    void given_a_short_ttl_when_it_expires_before_the_protected_method_finishes_then_a_later_call_can_acquire_again_standalone() throws Exception {
        IdempotencyKey key = new IdempotencyKey("ttl-expiry-" + UUID.randomUUID());

        Lease originalCall = standaloneRepository.tryAcquire(key, "hash", Duration.ofMillis(300));
        assertTrue(originalCall.isAcquired());

        Lease concurrentRetry = standaloneRepository.tryAcquire(key, "hash", Duration.ofSeconds(30));
        assertFalse(concurrentRetry.isAcquired());
        assertFalse(concurrentRetry.hasCachedResponse());

        Thread.sleep(600);

        Lease afterExpiry = standaloneRepository.tryAcquire(key, "hash", Duration.ofSeconds(30));
        assertTrue(afterExpiry.isAcquired(), "once the lease TTL elapses, a later call is allowed to acquire a new one");
    }

    // -----------------------------------------------------------------
    // P2 — AD-9: informative TPS, N distinct keys (R-004 mitigation), no floor asserted.
    // Manual wiring (own RedisTemplate/LettuceConnectionFactory, built in setUpAll above).
    // The DI-resolved counterpart lives in RedisIdempotentRepositoryTpsSpringContextITTest.
    // -----------------------------------------------------------------

    @Test
    void tpsInformativoComChavesDistintas() throws Exception {
        int operationCount = 5000;
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<Lease>> futures = new ArrayList<>();
        boolean completedNormally = false;
        try {
            List<Callable<Lease>> tasks = new ArrayList<>();
            for (int i = 0; i < operationCount; i++) {
                // R-004: a distinct key per operation, so the measured path is the real SET NX PX,
                // not the cheap in-progress GET a single shared key would mostly exercise.
                IdempotencyKey key = new IdempotencyKey("tps-" + UUID.randomUUID());
                tasks.add(() -> standaloneRepository.tryAcquire(key, "tps-payload-hash", Duration.ofSeconds(30)));
            }

            long start = System.nanoTime();
            assertDoesNotThrow(() -> {
                for (Callable<Lease> task : tasks) {
                    futures.add(pool.submit(task));
                }
                for (Future<Lease> future : futures) {
                    future.get(TASK_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
                }
            }, "no exception is expected during the TPS run (AD-9: informative only, never a floor)");
            long elapsedNanos = System.nanoTime() - start;

            double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
            double tps = operationCount / elapsedSeconds;
            System.out.printf(
                    "[AD-9][informative][no gate] Standalone tryAcquire TPS: %d ops in %.3fs = %.1f ops/s (distinct keys, real SET NX PX path)%n",
                    operationCount, elapsedSeconds, tps);

            assertEquals(operationCount, futures.size());
            completedNormally = true;
        } finally {
            // See the battery test above for why shutdownNow()+cancel replaces shutdown() on
            // any non-happy path (assertDoesNotThrow rethrows as an AssertionError, an Error,
            // not an Exception, so a plain catch(Exception) wouldn't have caught it anyway).
            if (completedNormally) {
                pool.shutdown();
            } else {
                pool.shutdownNow();
                futures.forEach(f -> f.cancel(true));
            }
        }
    }

    // -----------------------------------------------------------------
    // P3 — AD-9: informative TPS sweep by concurrency level (pedido direto do humano,
    // 2026-08-30): a infra mínima de produção prevista começa em torno de 100 conexões
    // reais com o banco; um único operationCount plano não mostra como o throughput se
    // move conforme o número de chamadores concorrentes cresce. Mesmo contrato
    // tryAcquire -> Lease, mesma mitigação de chaves distintas (R-004), continua
    // puramente informativo — nenhum piso mínimo é assertado em nenhum nível.
    // -----------------------------------------------------------------

    private static final int[] CONCURRENCY_LEVELS = {10, 30, 50, 100, 150, 300};
    private static final int OPERATIONS_PER_LEVEL = 10_000;

    @Test
    void tpsInformativoPorNivelDeConcorrencia() throws Exception {
        System.out.printf(
                "[AD-9][informative][no gate] Standalone tryAcquire TPS sweep by concurrency level (%d ops/level):%n",
                OPERATIONS_PER_LEVEL);
        for (int concurrency : CONCURRENCY_LEVELS) {
            double tps = runTpsAtConcurrencyLevel(concurrency);
            System.out.printf("  concurrency=%3d -> %d ops = %.1f ops/s%n", concurrency, OPERATIONS_PER_LEVEL, tps);
        }
    }

    private double runTpsAtConcurrencyLevel(int concurrency) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        List<Future<Lease>> futures = new ArrayList<>();
        boolean completedNormally = false;
        try {
            List<Callable<Lease>> tasks = new ArrayList<>();
            for (int i = 0; i < OPERATIONS_PER_LEVEL; i++) {
                // R-004: a distinct key per operation, so the measured path is the real SET NX PX,
                // not the cheap in-progress GET a single shared key would mostly exercise.
                IdempotencyKey key = new IdempotencyKey("tps-sweep-" + concurrency + "-" + UUID.randomUUID());
                tasks.add(() -> standaloneRepository.tryAcquire(key, "tps-sweep-payload-hash", Duration.ofSeconds(30)));
            }

            long start = System.nanoTime();
            assertDoesNotThrow(() -> {
                for (Callable<Lease> task : tasks) {
                    futures.add(pool.submit(task));
                }
                for (Future<Lease> future : futures) {
                    future.get(TASK_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
                }
            }, "no exception is expected during the TPS sweep (AD-9: informative only, never a floor), concurrency=" + concurrency);
            long elapsedNanos = System.nanoTime() - start;

            assertEquals(OPERATIONS_PER_LEVEL, futures.size());
            completedNormally = true;
            return OPERATIONS_PER_LEVEL / (elapsedNanos / 1_000_000_000.0);
        } finally {
            // Same shutdown()/shutdownNow()+cancel split as the tests above: shutdown() alone
            // wouldn't interrupt threads still blocked on Redis I/O past a timeout/failure.
            if (completedNormally) {
                pool.shutdown();
            } else {
                pool.shutdownNow();
                futures.forEach(f -> f.cancel(true));
            }
        }
    }
}
