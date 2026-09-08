
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
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 3.14: {@code ScosJdempotentRedisProperties} migrated from field-level {@code @Value} to
 * {@code @ConfigurationProperties(prefix = "scos.jdempotent.cache.redis")}. Confirms the binding
 * mechanism itself (not just that the fields still exist) resolves custom values, that
 * {@code persistReqRes} keeps its default when absent, and that {@code expirationTimeHour},
 * {@code dialTimeoutSecond}, {@code readTimeoutSecond}, {@code writeTimeoutSecond} and
 * {@code maxRetryCount} all keep failing fast when unconfigured — the same "no silent default"
 * behavior they already had under {@code @Value} (see Dev Notes, Story 3.14).
 *
 * <p>Review patch: the first cut of this test class only asserted the fail-fast for
 * {@code expirationTimeHour}, and {@link #assumePersistReqResPadraoQuandoNaoConfigurado} set only
 * that one property — passing even though the other 4 fields had silently lost their fail-fast
 * during the migration. Fixed both.</p>
 */
class ScosJdempotentRedisPropertiesTest {

    // RefreshAutoConfiguration registers the "refresh" Scope that @RefreshScope needs to resolve
    // its scoped-proxy target — present for real via spring-cloud-starter in production, but this
    // runner starts from a blank slate and must add it explicitly to invoke getters on the bean.
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class, RefreshAutoConfiguration.class))
            .withBean(ScosJdempotentRedisProperties.class, ScosJdempotentRedisProperties::new);

    @Test
    void vinculaValoresCustomizadosDeCacheRedis() {
        runner.withPropertyValues(
                "scos.jdempotent.cache.redis.expirationTimeHour=5",
                "scos.jdempotent.cache.redis.dialTimeoutSecond=7",
                "scos.jdempotent.cache.redis.readTimeoutSecond=8",
                "scos.jdempotent.cache.redis.writeTimeoutSecond=9",
                "scos.jdempotent.cache.redis.maxRetryCount=4",
                "scos.jdempotent.cache.redis.persistReqRes=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                ScosJdempotentRedisProperties properties = context.getBean(ScosJdempotentRedisProperties.class);
                assertThat(properties.getExpirationTimeHour()).isEqualTo(5L);
                assertThat(properties.getDialTimeoutSecond()).isEqualTo("7");
                assertThat(properties.getReadTimeoutSecond()).isEqualTo("8");
                assertThat(properties.getWriteTimeoutSecond()).isEqualTo("9");
                assertThat(properties.getMaxRetryCount()).isEqualTo("4");
                assertThat(properties.getPersistReqRes()).isFalse();
            });
    }

    @Test
    void assumePersistReqResPadraoQuandoNaoConfigurado() {
        runner.withPropertyValues(
                "scos.jdempotent.cache.redis.expirationTimeHour=1",
                "scos.jdempotent.cache.redis.dialTimeoutSecond=3",
                "scos.jdempotent.cache.redis.readTimeoutSecond=3",
                "scos.jdempotent.cache.redis.writeTimeoutSecond=3",
                "scos.jdempotent.cache.redis.maxRetryCount=3")
            .run(context -> {
                assertThat(context).hasNotFailed();
                ScosJdempotentRedisProperties properties = context.getBean(ScosJdempotentRedisProperties.class);
                assertThat(properties.getPersistReqRes()).isTrue();
            });
    }

    @Test
    void falhaDeFormaExplicitaQuandoExpirationTimeHourNaoEstaConfigurado() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("scos.jdempotent.cache.redis.expirationTimeHour");
        });
    }

    /**
     * Review patch: expirationTimeHour configured, dialTimeoutSecond (one of the other 4
     * previously-required fields) omitted — must still fail. Covers the regression the first cut
     * of this migration introduced for those 4 fields.
     */
    @Test
    void falhaDeFormaExplicitaQuandoDialTimeoutSecondNaoEstaConfigurado() {
        runner.withPropertyValues("scos.jdempotent.cache.redis.expirationTimeHour=1")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                        .hasStackTraceContaining("scos.jdempotent.cache.redis.dialTimeoutSecond");
            });
    }
}
