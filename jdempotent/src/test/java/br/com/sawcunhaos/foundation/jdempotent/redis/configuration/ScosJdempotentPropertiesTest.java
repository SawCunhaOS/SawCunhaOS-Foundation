
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

import br.com.sawcunhaos.foundation.jdempotent.core.aspect.IdempotentAspect;
import br.com.sawcunhaos.foundation.jdempotent.core.callback.ErrorConditionalCallback;
import br.com.sawcunhaos.foundation.jdempotent.core.generator.DefaultKeyGenerator;
import br.com.sawcunhaos.foundation.jdempotent.core.generator.KeyGenerator;
import br.com.sawcunhaos.foundation.jdempotent.core.metrics.IdempotencyMetrics;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.IdempotentTestPayload;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Story 3.10: o namespace de prefixo de chave passa a ser configurável via propriedade Spring
 * ({@code scos.jdempotent.namespace}), obrigatória — substitui a leitura silenciosa de
 * {@code System.getenv(APP_NAME)} de {@code DefaultKeyGenerator}, que hoje simplesmente omite o
 * prefixo quando a variável de ambiente não está definida (risco de colisão de chaves entre
 * aplicações diferentes compartilhando o mesmo Redis).
 */
class ScosJdempotentPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ScosJdempotentRedisProperties.class, ScosJdempotentRedisProperties::new)
            .withBean("JdempotentRedisTemplate", RedisTemplate.class, () -> mock(RedisTemplate.class))
            // Story 3.14: expirationTimeHour is a required @ConfigurationProperties field (no
            // default, same as it was under @Value) — set here so it doesn't interfere with the
            // namespace assertions below, which are what these tests are actually about.
            .withPropertyValues("scos.jdempotent.cache.redis.expirationTimeHour=1")
            // Story 3.11: ScosJdempotentConfig now constructor-injects IdempotencyMetrics —
            // ScosJdempotentMetricsConfiguration is what supplies it (no-op here, no Micrometer
            // bean registered in this runner).
            .withUserConfiguration(ScosJdempotentMetricsConfiguration.class, ScosJdempotentConfig.class);

    @Test
    void falhaDeFormaExplicitaQuandoNamespaceNaoEstaConfigurado() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            // Nem NullPointerException genérico nem silêncio: a causa raiz é uma
            // BindValidationException (@NotBlank) citando a property, embrulhada em
            // ConfigurationPropertiesBindException / UnsatisfiedDependencyException por cima.
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("scos.jdempotent.namespace")
                    .hasStackTraceContaining("NotBlank");
        });
    }

    @Test
    void aplicaOPrefixoDeNamespaceConfiguradoNaChaveGerada() {
        runner.withPropertyValues("scos.jdempotent.namespace=checkout-service")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    ScosJdempotentProperties properties = context.getBean(ScosJdempotentProperties.class);
                    assertThat(properties.getNamespace()).isEqualTo("checkout-service");

                    // DefaultKeyGenerator não lê mais variável de ambiente: recebe o namespace já
                    // resolvido pela configuração Spring, exatamente como ScosJdempotentConfig o
                    // constrói para o bean IdempotentAspect real.
                    DefaultKeyGenerator keyGenerator = new DefaultKeyGenerator(properties.getNamespace());
                    IdempotencyKey key = keyGenerator.generateIdempotentKey(
                            new IdempotentRequestWrapper(new IdempotentTestPayload("payload")),
                            "listener", new StringBuilder(), MessageDigest.getInstance("SHA-256"));

                    assertThat(key.getKeyValue()).startsWith("checkout-service-listener-");
                });
    }

    /**
     * Cobre {@code getIdempotentAspectOnErrorConditionalCallback} especificamente: sem este teste,
     * uma regressão que trocasse {@code new DefaultKeyGenerator(jdempotentProperties.getNamespace())}
     * de volta para {@code new DefaultKeyGenerator()} nesse bean (o único dos dois que exige um
     * {@code ErrorConditionalCallback} registrado) não seria detectada por nenhum teste existente.
     */
    @Test
    void aplicaOPrefixoDeNamespaceNoBeanComErrorConditionalCallback() {
        runner.withPropertyValues("scos.jdempotent.namespace=checkout-service")
                .withBean(ErrorConditionalCallback.class, () -> mock(ErrorConditionalCallback.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    IdempotentAspect aspect = context.getBean(IdempotentAspect.class);
                    KeyGenerator keyGenerator = (KeyGenerator) ReflectionTestUtils.getField(aspect, "keyGenerator");
                    IdempotencyKey key = keyGenerator.generateIdempotentKey(
                            new IdempotentRequestWrapper(new IdempotentTestPayload("payload")),
                            "listener", new StringBuilder(), MessageDigest.getInstance("SHA-256"));

                    assertThat(key.getKeyValue()).startsWith("checkout-service-listener-");

                    // Story 3.11 (review finding #6): the getIdempotentAspectOnErrorConditionalCallback
                    // bean specifically must also wire the resolved IdempotencyMetrics through.
                    assertThat(aspect.getIdempotencyMetrics()).isSameAs(context.getBean(IdempotencyMetrics.class));
                });
    }
}
