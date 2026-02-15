
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


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;

/**
 *
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix="scos.jdempotent", name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@RefreshScope
@Data
public class ScosJdempotentRedisProperties {

    @Value("${scos.jdempotent.enabled:false}")
    private boolean enable;

    @Value("${scos.jdempotent.cache.redis.expirationTimeHour}")
    private Long expirationTimeHour;

    @Value("${scos.jdempotent.cache.redis.dialTimeoutSecond}")
    private String dialTimeoutSecond;

    @Value("${scos.jdempotent.cache.redis.readTimeoutSecond}")
    private String readTimeoutSecond;

    @Value("${scos.jdempotent.cache.redis.writeTimeoutSecond}")
    private String writeTimeoutSecond;

    @Value("${scos.jdempotent.cache.redis.maxRetryCount}")
    private String maxRetryCount;

    @Value("${scos.jdempotent.cache.redis.persistReqRes:true}")
    private Boolean persistReqRes;
}
