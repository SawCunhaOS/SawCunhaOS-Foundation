
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Story 3.11 (FR7): registers the single {@link IdempotencyMetrics} bean the rest of
 * the module (see {@code ScosJdempotentConfig}) wires into {@code IdempotentAspect}
 * and {@code RedisIdempotentRepository}.
 *
 * <p>Two candidates, in this order:</p>
 * <ol>
 *   <li>{@link #micrometerIdempotencyMetrics}: only considered when {@code MeterRegistry}
 *       (Micrometer) is on the classpath ({@code @ConditionalOnClass}) — the exact
 *       mechanism the AC asks for. An {@link ObjectProvider} is used instead of a
 *       direct {@code MeterRegistry} parameter so this still resolves safely (falling
 *       back to no-op) even if Micrometer is present but no {@code MeterRegistry} bean
 *       has been configured (e.g. actuator not enabled), regardless of the relative
 *       auto-configuration ordering between this module and actuator's. {@code getIfUnique()},
 *       not {@code getIfAvailable()}: a consumer with 2+ non-{@code @Primary} {@code MeterRegistry}
 *       beans (e.g. Prometheus + CloudWatch exporters) must fall back to no-op too, not blow up
 *       context startup with {@code NoUniqueBeanDefinitionException} (review finding #2).</li>
 *   <li>{@link #noOpIdempotencyMetrics}: the default, {@code @ConditionalOnMissingBean}
 *       so it only applies when the bean above did not register — either because
 *       Micrometer is absent, or a consumer supplied its own {@code IdempotencyMetrics}
 *       bean.</li>
 * </ol>
 */
@AutoConfiguration
public class ScosJdempotentMetricsConfiguration {

    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnMissingBean(IdempotencyMetrics.class)
    public IdempotencyMetrics micrometerIdempotencyMetrics(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        // getIfUnique(), not getIfAvailable(): 2+ non-@Primary MeterRegistry beans must fall back
        // to no-op (null), not throw NoUniqueBeanDefinitionException and take the context down.
        MeterRegistry registry = meterRegistryProvider.getIfUnique();
        return registry != null ? new MicrometerIdempotencyMetrics(registry) : new NoOpIdempotencyMetrics();
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyMetrics.class)
    public IdempotencyMetrics noOpIdempotencyMetrics() {
        return new NoOpIdempotencyMetrics();
    }
}
