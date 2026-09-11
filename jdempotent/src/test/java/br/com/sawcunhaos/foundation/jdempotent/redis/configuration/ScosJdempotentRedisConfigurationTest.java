
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

package br.com.sawcunhaos.foundation.jdempotent.redis.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre a Story 3.1: {@code lettuceConnectionFactory()} não pode mais assumir Sentinel
 * incondicionalmente — a inicialização precisa funcionar em standalone/cluster sem
 * o bloco {@code spring.data.redis.sentinel.*}, e continuar funcionando quando ele existe.
 */
class ScosJdempotentRedisConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ScosJdempotentRedisConfiguration.class);

    @Test
    void inicializaSemNPEComTopologiaStandalone() {
        runner.withPropertyValues(
                        "spring.data.redis.host=localhost",
                        "spring.data.redis.port=6379",
                        "spring.data.redis.password=secret",
                        "spring.data.redis.database=2")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(LettuceConnectionFactory.class);

                    LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
                    assertThat(factory.isRedisSentinelAware()).isFalse();
                    assertThat(factory.isClusterAware()).isFalse();
                    assertThat(factory.getPassword()).isEqualTo("secret");
                    assertThat(factory.getDatabase()).isEqualTo(2);
                });
    }

    @Test
    void inicializaSemNPEComTopologiaCluster() {
        runner.withPropertyValues(
                        "spring.data.redis.cluster.nodes[0]=localhost:7000",
                        "spring.data.redis.cluster.nodes[1]=localhost:7001",
                        "spring.data.redis.cluster.max-redirects=3",
                        "spring.data.redis.password=secret")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(LettuceConnectionFactory.class);

                    LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
                    assertThat(factory.isClusterAware()).isTrue();
                    assertThat(factory.isRedisSentinelAware()).isFalse();
                    assertThat(factory.getClusterConfiguration().getMaxRedirects()).isEqualTo(3);
                    assertThat(factory.getPassword()).isEqualTo("secret");
                });
    }

    @Test
    void continuaFuncionandoComTopologiaSentinel() {
        runner.withPropertyValues(
                        "spring.data.redis.sentinel.master=jdempotent",
                        "spring.data.redis.sentinel.nodes[0]=localhost:26379")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(LettuceConnectionFactory.class);

                    LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
                    assertThat(factory.isRedisSentinelAware()).isTrue();
                    assertThat(factory.isClusterAware()).isFalse();
                });
    }

    /**
     * Cobre a Story 3.18/AD-8: configuração malformada (tipo inválido, sem depender de rede)
     * deve falhar a subida do contexto com causa raiz identificável, nunca silenciosamente.
     * Não cobre "Redis alcançável mas fora do ar" — esse é fail-open (AD-2 herdado).
     * Ver o Architecture Spine 2026-08-29 da Foundation, no workspace SCOS (fora deste repo):
     * {@code _bmad-output/SawCunhaOS-Foundation/planning-artifacts/architecture/architecture-SawCunhaOS-Foundation-2026-08-29/}.
     */
    @Test
    void contextoFalhaComConfigClusterMalformada() {
        runner.withPropertyValues(
                        "spring.data.redis.cluster.nodes[0]=localhost:7000",
                        "spring.data.redis.cluster.max-redirects=not-a-number")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("not-a-number");
                });
    }

    /**
     * Story 3.19/AD-8 estendido: mesma garantia de fail-fast da 3.18 (Cluster), agora para
     * Standalone — {@code spring.data.redis.port} é {@code int} em {@code DataRedisProperties},
     * então um valor não-numérico falha o binding do Spring Boot antes mesmo do bean de
     * {@code ScosJdempotentRedisConfiguration} rodar (R-005, verificado nesta story rodando
     * este teste isoladamente).
     */
    @Test
    void contextoFalhaComConfigStandaloneMalformada() {
        runner.withPropertyValues(
                        "spring.data.redis.host=localhost",
                        "spring.data.redis.port=not-a-number")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("not-a-number");
                });
    }

    /**
     * Story 3.19/AD-8 estendido: mesma garantia de fail-fast da 3.18 (Cluster), agora para
     * Sentinel — a porta não-numérica em {@code sentinel.nodes[0]} dispara
     * {@code NumberFormatException} dentro de {@code sentinelConfiguration()}
     * ({@code Integer.parseInt(parts[1])}), sem depender de rede.
     */
    @Test
    void contextoFalhaComConfigSentinelMalformada() {
        runner.withPropertyValues(
                        "spring.data.redis.sentinel.master=jdempotent",
                        "spring.data.redis.sentinel.nodes[0]=localhost:not-a-port")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("not-a-port");
                });
    }
}
