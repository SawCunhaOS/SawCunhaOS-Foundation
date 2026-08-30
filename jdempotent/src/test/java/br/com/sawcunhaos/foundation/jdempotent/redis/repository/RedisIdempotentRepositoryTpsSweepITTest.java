
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

import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.Lease;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Story 3.19 / AD-9 (pedido direto do humano, 2026-08-30): sweep informativo de TPS por
 * nível de concorrência — mesmo contrato {@code tryAcquire -> Lease}, chaves distintas
 * (R-004), nunca um piso mínimo assertado. Cada nível roda 1 passagem de warm-up descartada
 * (mitiga viés de JIT/conexão fria) + {@value #MEASURED_REPETITIONS} medições reais, reportando
 * média e desvio-padrão em vez de um único valor por nível (uma execução isolada mostrou
 * variação grande demais entre rodadas para ser confiável sozinha). Movido para sua própria
 * classe/perfil porque, a {@code 100_000} operações por nível × 6 níveis × 6 passagens
 * (warm-up + 5 medições) = 3,6 milhões de operações, é caro demais (~4 min) para rodar em
 * todo {@code mvn verify} — gatilho: {@link #CONCURRENCY_LEVELS}/{@link #OPERATIONS_PER_LEVEL}.
 *
 * <p><b>Só roda com o profile Maven {@code tps-sweep}</b> ({@code mvn -Ptps-sweep -pl jdempotent verify}):
 * {@code jdempotent/pom.xml} exclui esta classe do Failsafe por padrão e só a inclui quando o
 * profile está ativo, então ela nunca atrasa o {@code mvn verify} normal nem o CI padrão.</p>
 */
@Testcontainers
class RedisIdempotentRepositoryTpsSweepITTest {

    private static final Duration TASK_TIMEOUT = Duration.ofSeconds(30);
    private static final int[] CONCURRENCY_LEVELS = {10, 30, 50, 100, 150, 300};
    private static final int OPERATIONS_PER_LEVEL = 100_000;
    // Story 3.19 (pedido direto do humano, 2026-08-30): 1 warm-up descartada (mitiga o viés de
    // JIT/warm-up de conexão apontado na avaliação crítica dos resultados de execução única) +
    // 5 medições reais por nível, reportando média e desvio-padrão em vez de um único valor.
    private static final int WARMUP_REPETITIONS = 1;
    private static final int MEASURED_REPETITIONS = 5;

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
        standaloneRepository = RedisIdempotentRepositoryTopologyITTest.repositoryUsing(standaloneConnectionFactory);
    }

    @AfterAll
    static void tearDownAll() {
        standaloneConnectionFactory.destroy();
    }

    @Test
    void tpsInformativoPorNivelDeConcorrencia() throws Exception {
        System.out.printf(
                "[AD-9][informative][no gate] Standalone tryAcquire TPS sweep by concurrency level "
                        + "(%d ops/level, %d warm-up + %d measured repetitions):%n",
                OPERATIONS_PER_LEVEL, WARMUP_REPETITIONS, MEASURED_REPETITIONS);
        for (int concurrency : CONCURRENCY_LEVELS) {
            for (int warmup = 0; warmup < WARMUP_REPETITIONS; warmup++) {
                runTpsAtConcurrencyLevel(concurrency); // descartada de propósito, só aquece JIT/conexão
            }

            double[] measurements = new double[MEASURED_REPETITIONS];
            for (int repetition = 0; repetition < MEASURED_REPETITIONS; repetition++) {
                measurements[repetition] = runTpsAtConcurrencyLevel(concurrency);
            }

            double mean = Arrays.stream(measurements).average().orElseThrow();
            double variance = Arrays.stream(measurements)
                    .map(value -> (value - mean) * (value - mean))
                    .sum() / (MEASURED_REPETITIONS - 1);
            double stdDev = Math.sqrt(variance);

            String measurementsFormatted = Arrays.stream(measurements)
                    .mapToObj("%.1f"::formatted)
                    .collect(Collectors.joining(", "));
            System.out.printf(
                    "  concurrency=%3d -> mean=%.1f ops/s, stddev=%.1f (n=%d, medições=[%s])%n",
                    concurrency, mean, stdDev, MEASURED_REPETITIONS, measurementsFormatted);
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
            // Same shutdown()/shutdownNow()+cancel split used by the sibling TPS tests: shutdown()
            // alone wouldn't interrupt threads still blocked on Redis I/O past a timeout/failure.
            if (completedNormally) {
                pool.shutdown();
            } else {
                pool.shutdownNow();
                futures.forEach(f -> f.cancel(true));
            }
        }
    }
}
