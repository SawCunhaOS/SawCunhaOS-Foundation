
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

import br.com.sawcunhaos.foundation.jdempotent.core.metrics.IdempotencyMetrics;
import br.com.sawcunhaos.foundation.jdempotent.core.metrics.MicrometerIdempotencyMetrics;
import br.com.sawcunhaos.foundation.jdempotent.core.metrics.NoOpIdempotencyMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 3.11 (FR7), Task 2/4: {@link IdempotencyMetrics} is no-op by default and only
 * becomes Micrometer-backed when Micrometer is actually usable — never a startup error
 * either way.
 */
class ScosJdempotentMetricsConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ScosJdempotentMetricsConfiguration.class));

    @Test
    void given_micrometer_absent_from_the_classpath_when_the_context_loads_then_the_noop_metrics_bean_is_used_without_error() {
        // FilteredClassLoader simulates a consumer without Micrometer on the classpath at all —
        // the module must not fail context startup nor demand a bean that cannot exist.
        runner.withClassLoader(new FilteredClassLoader(MeterRegistry.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(IdempotencyMetrics.class)).isInstanceOf(NoOpIdempotencyMetrics.class);
                });
    }

    @Test
    void given_micrometer_on_the_classpath_but_no_meter_registry_bean_when_the_context_loads_then_the_noop_metrics_bean_is_used() {
        // Micrometer classes present (unfiltered classloader) but no MeterRegistry bean
        // registered (e.g. actuator not configured) — must still fall back to no-op, not fail.
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(IdempotencyMetrics.class)).isInstanceOf(NoOpIdempotencyMetrics.class);
        });
    }

    @Test
    void given_micrometer_on_the_classpath_with_a_meter_registry_bean_when_the_context_loads_then_the_micrometer_metrics_bean_is_used() {
        runner.withUserConfiguration(MeterRegistryConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(IdempotencyMetrics.class)).isInstanceOf(MicrometerIdempotencyMetrics.class);
                });
    }

    @Configuration
    static class MeterRegistryConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
