
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


import br.com.sawcunhaos.foundation.jdempotent.core.datasource.IdempotentRepository;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.redis.configuration.ScosJdempotentRedisProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

/**
 *
 * An implementation of the idempotent IdempotentRepository
 * that uses a distributed hash map from Redis
 *
 * That repository needs to store idempotent hash for idempotency check
 *
 */
@Slf4j
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "RedisTemplate and the @ConfigurationProperties bean are Spring-injected collaborators stored by reference by design; they are not value objects to be copied.")
public class RedisIdempotentRepository implements IdempotentRepository {

    private final ValueOperations<String, IdempotentRequestResponseWrapper> valueOperations;
    private final RedisTemplate redisTemplate;
    private final ScosJdempotentRedisProperties redisProperties;


    public RedisIdempotentRepository(@Qualifier("JdempotentRedisTemplate") RedisTemplate redisTemplate, ScosJdempotentRedisProperties redisProperties) {
        this.valueOperations = redisTemplate.opsForValue();
        this.redisTemplate = redisTemplate;
        this.redisProperties = redisProperties;
    }

    @Override
    public boolean contains(IdempotencyKey idempotencyKey) {
        try {
            return this.valueOperations.get(idempotencyKey.getKeyValue()) != null;
        } catch (Exception e) {
            log.error("Error checking idempotency key in Redis: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public IdempotentResponseWrapper getResponse(IdempotencyKey idempotencyKey) {
        try {
            return this.valueOperations.get(idempotencyKey.getKeyValue()).getResponse();
        } catch (Exception e) {
            log.error("Error retrieving idempotent response from Redis: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void store(IdempotencyKey idempotencyKey, IdempotentRequestWrapper request, Long ttl, TimeUnit timeUnit) {
        try {
            ttl = ttl == 0 ? redisProperties.getExpirationTimeHour() : ttl;
            this.valueOperations.set(idempotencyKey.getKeyValue(), prepareValue(request), ttl, timeUnit);
        } catch (Exception e) {
            log.error("Error storing idempotent request in Redis: {}", e.getMessage());
        }
    }

    @Override
    public void remove(IdempotencyKey idempotencyKey) {
        try {
            redisTemplate.delete(idempotencyKey.getKeyValue());
        } catch (Exception e) {
            log.error("Error removing idempotent key from Redis: {}", e.getMessage());
        }
    }

    /**
     * ttl describe
     *
     * @param idempotencyKey
     * @param request
     * @param response
     * @param ttl
     */
    @Override
    public void setResponse(IdempotencyKey idempotencyKey, IdempotentRequestWrapper request, IdempotentResponseWrapper response, Long ttl, TimeUnit timeUnit) {
        try {
            if (contains(idempotencyKey)) {
                ttl = ttl == 0 ? redisProperties.getExpirationTimeHour() : ttl;
                IdempotentRequestResponseWrapper requestResponseWrapper = valueOperations.get(idempotencyKey.getKeyValue());
                requestResponseWrapper.setResponse(response);
                this.valueOperations.set(idempotencyKey.getKeyValue(), prepareValue(request, response), ttl, timeUnit);
            }
        } catch (Exception e) {
            log.error("Error setting idempotent response in Redis: {}", e.getMessage());
        }
    }

    /**
     * Prepares the value stored in redis
     *
     * if persistReqRes set to false,
     * it does not persist related request values in redis
     * @param request
     * @return
     */
    private IdempotentRequestResponseWrapper prepareValue(IdempotentRequestWrapper request) {
        if (redisProperties.getPersistReqRes()) {
            return new IdempotentRequestResponseWrapper(request);
        }
        return new IdempotentRequestResponseWrapper(null);
    }

    /**
     * Prepares the value stored in redis
     *
     * if persistReqRes set to false,
     * it does not persist related request and response values in redis
     * @param request
     * @param response
     * @return
     */
    private IdempotentRequestResponseWrapper prepareValue(IdempotentRequestWrapper request, IdempotentResponseWrapper response) {
        if (redisProperties.getPersistReqRes()) {
            return new IdempotentRequestResponseWrapper(request, response);
        }
        return new IdempotentRequestResponseWrapper(null);
    }
}

