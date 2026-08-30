
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

import br.com.sawcunhaos.foundation.jdempotent.core.aspect.IdempotentAspect;
import br.com.sawcunhaos.foundation.jdempotent.core.datasource.IdempotentRepository;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.Lease;
import br.com.sawcunhaos.foundation.jdempotent.redis.test.app.JdempotentTestApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Story 3.19 (Emenda 2026-08-30, item 4): the same informative TPS measurement as
 * {@code RedisIdempotentRepositoryTopologyITTest#tpsInformativoComChavesDistintas}, but resolving
 * the {@link IdempotentRepository} through a real Spring context / DI (reusing
 * {@link JdempotentTestApplication}, the same test app {@code PrimeNumbersJdempotentEnableITTest}
 * uses) instead of hand-building a {@code RedisTemplate}/{@code LettuceConnectionFactory} — proving
 * the autoconfiguration path, not just the manual-wiring one. Does not replace the manual test,
 * which keeps covering the repository contract in isolation from Spring.
 *
 * <p>Kept in its own class/context rather than folded into the manual-wiring one: {@code @SpringBootTest}
 * is class-scoped, and forcing a Spring context onto the manual tests would contradict their
 * "no Spring DI" intent and needlessly slow them down.</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
            JdempotentTestApplication.class
        })
class RedisIdempotentRepositoryTpsSpringContextITTest {

    private static final Duration TASK_TIMEOUT = Duration.ofSeconds(30);

    // Not @Container/@Testcontainers-managed: Spring resolves spring.data.redis.* while building
    // the ApplicationContext, so the container must already be running by the time
    // @DynamicPropertySource runs (before @Testcontainers' @BeforeAll would fire) — started
    // explicitly below instead.
    static GenericContainer<?> standaloneRedis = new GenericContainer<>("bitnami/redis:latest")
            .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        standaloneRedis.start();
        registry.add("spring.data.redis.host", standaloneRedis::getHost);
        registry.add("spring.data.redis.port", () -> standaloneRedis.getMappedPort(6379));
    }

    @AfterAll
    static void tearDown() {
        standaloneRedis.stop();
    }

    @Autowired
    private IdempotentAspect idempotentAspect;

    @Test
    void tpsInformativoViaContextoSpring() throws Exception {
        IdempotentRepository repository = idempotentAspect.getIdempotentRepository();
        int operationCount = 500;
        ExecutorService pool = Executors.newFixedThreadPool(20);
        List<Future<Lease>> futures = new ArrayList<>();
        boolean completedNormally = false;
        try {
            List<Callable<Lease>> tasks = new ArrayList<>();
            for (int i = 0; i < operationCount; i++) {
                // R-004: a distinct key per operation, same reasoning as the manual TPS test.
                IdempotencyKey key = new IdempotencyKey("tps-spring-" + UUID.randomUUID());
                tasks.add(() -> repository.tryAcquire(key, "tps-spring-payload-hash", Duration.ofSeconds(30)));
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
                    "[AD-9][informative][no gate][spring-context] tryAcquire TPS: %d ops in %.3fs = %.1f ops/s (DI-resolved repository)%n",
                    operationCount, elapsedSeconds, tps);

            assertEquals(operationCount, futures.size());
            completedNormally = true;
        } finally {
            // shutdown() alone doesn't interrupt threads still blocked on Redis I/O — they'd
            // keep running against a container @AfterAll may already be tearing down.
            if (completedNormally) {
                pool.shutdown();
            } else {
                pool.shutdownNow();
                futures.forEach(f -> f.cancel(true));
            }
        }
    }
}
