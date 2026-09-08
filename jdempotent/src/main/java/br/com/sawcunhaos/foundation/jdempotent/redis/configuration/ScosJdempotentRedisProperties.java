
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


import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;

/**
 * Story 3.14: migrated from field-level {@code @Value} to
 * {@code @ConfigurationProperties(prefix = "scos.jdempotent.cache.redis")} — typed, centralized
 * binding instead of loose {@code @Value} lookups scattered across fields.
 *
 * <p>{@code @AutoConfiguration}/{@code @ConditionalOnProperty} are kept (unchanged from before this
 * migration): this class is still self-registered via {@code AutoConfiguration.imports}, gated on
 * {@code scos.jdempotent.enabled}, exactly like the sibling {@code ScosJdempotentConfig} bean that
 * consumes it — removing either conditional here without touching the other would leave
 * {@code ScosJdempotentConfig} injecting a bean that no longer exists in the disabled case.</p>
 *
 * <p>The old {@code enable} field (bound to {@code scos.jdempotent.enabled} via {@code @Value})
 * was removed: it had no getter and was never read anywhere (a dead property, same category the
 * AC calls out), and its property key never actually belonged under this class's
 * {@code cache.redis} prefix in the first place — the {@code enabled} flag is already the
 * condition every auto-configuration class in this module checks directly via
 * {@code @ConditionalOnProperty}, not a value consumers ever needed to read back from this bean.</p>
 *
 * <p>{@code expirationTimeHour}, {@code dialTimeoutSecond}, {@code readTimeoutSecond},
 * {@code writeTimeoutSecond} and {@code maxRetryCount} all keep failing fast when unconfigured
 * (they already did under {@code @Value} with no default) via the same two-layer pattern used by
 * {@code ScosJdempotentProperties} (Story 3.10): {@code @NotNull}/{@code @Validated} when a
 * JSR-380 provider is on the consumer's classpath, plus an unconditional {@code @PostConstruct}
 * check so the failure does not depend on one being present (this module only declares
 * {@code hibernate-validator} in {@code test} scope). Review patch: the first cut of this
 * migration only applied this to {@code expirationTimeHour}, silently dropping fail-fast for the
 * other 4 — fixed here.</p>
 */
@ConfigurationProperties(prefix = "scos.jdempotent.cache.redis")
@Validated
@AutoConfiguration
@ConditionalOnProperty(
        prefix="scos.jdempotent", name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@RefreshScope
@Data
public class ScosJdempotentRedisProperties {

    @NotNull(message = "scos.jdempotent.cache.redis.expirationTimeHour deve ser configurado (sem valor default)")
    private Long expirationTimeHour;

    @NotNull(message = "scos.jdempotent.cache.redis.dialTimeoutSecond deve ser configurado (sem valor default)")
    private String dialTimeoutSecond;

    @NotNull(message = "scos.jdempotent.cache.redis.readTimeoutSecond deve ser configurado (sem valor default)")
    private String readTimeoutSecond;

    @NotNull(message = "scos.jdempotent.cache.redis.writeTimeoutSecond deve ser configurado (sem valor default)")
    private String writeTimeoutSecond;

    @NotNull(message = "scos.jdempotent.cache.redis.maxRetryCount deve ser configurado (sem valor default)")
    private String maxRetryCount;

    private Boolean persistReqRes = true;

    @PostConstruct
    public void validateRequiredProperties() {
        requireConfigured(expirationTimeHour, "expirationTimeHour");
        requireConfigured(dialTimeoutSecond, "dialTimeoutSecond");
        requireConfigured(readTimeoutSecond, "readTimeoutSecond");
        requireConfigured(writeTimeoutSecond, "writeTimeoutSecond");
        requireConfigured(maxRetryCount, "maxRetryCount");
    }

    private void requireConfigured(Object value, String propertyName) {
        if (value == null) {
            throw new IllegalStateException(
                    "scos.jdempotent.cache.redis." + propertyName + " deve ser configurado (sem valor default)");
        }
    }
}
