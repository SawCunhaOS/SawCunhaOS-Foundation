
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
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 3.14: {@link ScosJdempotentRedisEnvironmentPostProcessor} used to be declared in
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} — a
 * SPI read only during {@code ApplicationContext} refresh. {@link org.springframework.boot.EnvironmentPostProcessor}
 * is a different, earlier SPI, read by {@code SpringApplication} before any
 * {@code ApplicationContext} exists — so {@code postProcessEnvironment()} was never actually
 * invoked by the old registration. It is now registered in {@code META-INF/spring.factories}
 * (see that resource's comment for why, not the {@code .imports}-file convention the Dev Notes
 * describe, which does not apply to this interface on this project's pinned Spring Boot version).
 *
 * <p>Deliberately boots a bare {@code SpringApplication} with no {@code @EnableAutoConfiguration}
 * (an empty {@code @Configuration} class as the only source): this SPI is read unconditionally for
 * every {@code SpringApplication}, regardless of auto-configuration, so none of this module's
 * Redis/idempotent auto-configuration classes get instantiated here — no live Redis needed, no
 * {@code scos.jdempotent.namespace} required. This test fails against the old registration (the
 * property below is never set) and passes once the class is registered in the correct SPI file.</p>
 */
class ScosJdempotentRedisEnvironmentPostProcessorRegistrationTest {

    @Configuration
    static class EmptyConfig {
    }

    @Test
    void defineSpringDataRedisRepositoriesEnabledComoFalseNaSubidaDaAplicacao() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(EmptyConfig.class)
                .web(WebApplicationType.NONE)
                .run()) {
            assertThat(context.getEnvironment().getProperty("spring.data.redis.repositories.enabled"))
                    .isEqualTo("false");
        }
    }

    /**
     * Review patch: the opt-out documented in the CHANGELOG ({@code scos.jdempotent.enabled=false})
     * had no coverage at all — neither this class nor {@code PrimeNumbersJdempotentDisableITTest}
     * asserted what the post-processor does to {@code spring.data.redis.repositories.enabled} in
     * that case. It must leave the property untouched, not force it to {@code "false"}.
     *
     * <p>Passed as a {@code --command-line} arg, not {@code SpringApplicationBuilder.properties(...)}
     * — that method sets low-precedence default properties, which this module's own
     * {@code src/test/resources/application.yml} (on the test classpath, {@code scos.jdempotent.enabled: true})
     * would otherwise override, silently defeating this test (confirmed: it failed with
     * {@code "false"} instead of {@code null} using {@code .properties(...)}).</p>
     */
    @Test
    void naoForcaSpringDataRedisRepositoriesEnabledQuandoScosJdempotentEstaDesabilitado() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(EmptyConfig.class)
                .web(WebApplicationType.NONE)
                .run("--scos.jdempotent.enabled=false")) {
            assertThat(context.getEnvironment().getProperty("spring.data.redis.repositories.enabled"))
                    .isNull();
        }
    }
}
