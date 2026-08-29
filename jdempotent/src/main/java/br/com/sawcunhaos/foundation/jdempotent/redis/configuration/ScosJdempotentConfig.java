
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
import br.com.sawcunhaos.foundation.jdempotent.redis.repository.RedisIdempotentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 *
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix="scos.jdempotent", name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class ScosJdempotentConfig {

    private final ScosJdempotentRedisProperties redisProperties;

    @Bean
    @ConditionalOnProperty(
            prefix="scos.jdempotent", name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    @ConditionalOnBean(ErrorConditionalCallback.class)
    public IdempotentAspect getIdempotentAspectOnErrorConditionalCallback(@Qualifier("JdempotentRedisTemplate") RedisTemplate redisTemplate, ErrorConditionalCallback errorConditionalCallback) {
        return new IdempotentAspect(new RedisIdempotentRepository(redisTemplate, redisProperties), errorConditionalCallback);
    }

    @Bean
    @ConditionalOnMissingBean(IdempotentAspect.class)
    public IdempotentAspect getIdempotentAspect(@Qualifier("JdempotentRedisTemplate") RedisTemplate redisTemplate) {
        return new IdempotentAspect(new RedisIdempotentRepository(redisTemplate, redisProperties));
    }

}
