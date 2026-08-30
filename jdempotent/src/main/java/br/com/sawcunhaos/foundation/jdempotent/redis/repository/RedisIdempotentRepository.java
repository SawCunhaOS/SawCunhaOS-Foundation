
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
import br.com.sawcunhaos.foundation.jdempotent.core.model.Lease;
import br.com.sawcunhaos.foundation.jdempotent.redis.configuration.ScosJdempotentRedisProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
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

    /**
     * Atomic lock acquisition (Story 3.5): {@code SET key value NX PX <ttl_ms>} is a
     * single Redis command, so exactly one concurrent caller for the same key can
     * ever see {@code acquired == true} — this is what replaces the racy
     * {@code contains() -> store()} sequence. Spring Data's {@code setIfAbsent(key, value, ttl)}
     * is that exact command; a hand-rolled Lua script is not needed here because this
     * story does not require comparing {@code payloadHash} atomically with the SET
     * itself (that only becomes necessary if Story 3.6 needs cross-field atomicity).
     *
     * <p>The follow-up {@code GET} used to populate {@link Lease#getExistingPayloadHash()}
     * / {@link Lease#getExistingResponse()} on conflict is NOT part of that atomic
     * operation: by the time it runs the key's owner may already have released or
     * refreshed it. That is fine for this story's AC (exclusivity of the lock is what
     * matters), it can only make the returned "existing" data slightly stale/absent.</p>
     *
     * <p>The payload-collision check (Story 3.6, AC #1/#2) reuses that same follow-up
     * {@code GET} — it is not a further round trip — and is decided before choosing
     * between {@link Lease#inProgress} and a cached response, so a mismatch always
     * takes precedence over both.</p>
     */
    @Override
    public Lease tryAcquire(IdempotencyKey idempotencyKey, String payloadHash, Duration ttl) {
        Duration effectiveTtl = (ttl == null || ttl.isZero() || ttl.isNegative())
                ? Duration.ofHours(redisProperties.getExpirationTimeHour())
                : ttl;
        try {
            IdempotentRequestResponseWrapper placeholder = prepareValue(null, payloadHash);
            Boolean acquired = valueOperations.setIfAbsent(idempotencyKey.getKeyValue(), placeholder, effectiveTtl);
            if (Boolean.TRUE.equals(acquired)) {
                return Lease.acquired(idempotencyKey, payloadHash, effectiveTtl);
            }
            IdempotentRequestResponseWrapper existing = valueOperations.get(idempotencyKey.getKeyValue());
            String existingPayloadHash = existing != null ? existing.getPayloadHash() : null;
            IdempotentResponseWrapper existingResponse = existing != null ? existing.getResponse() : null;
            if (existingPayloadHash != null && !existingPayloadHash.equals(payloadHash)) {
                return Lease.mismatch(idempotencyKey, payloadHash, effectiveTtl, existingPayloadHash, existingResponse);
            }
            return Lease.inProgress(idempotencyKey, payloadHash, effectiveTtl, existingPayloadHash, existingResponse);
        } catch (Exception e) {
            log.error("Error acquiring idempotent lease in Redis: {}", e.getMessage());
            // NOTE: fail-open on Redis errors, mirrors contains()/store() above which already
            // swallow exceptions and let the caller proceed rather than blocking on a Redis outage.
            return Lease.acquired(idempotencyKey, payloadHash, effectiveTtl);
        }
    }

    private IdempotentRequestResponseWrapper prepareValue(IdempotentRequestWrapper request, String payloadHash) {
        IdempotentRequestResponseWrapper wrapper = redisProperties.getPersistReqRes()
                ? new IdempotentRequestResponseWrapper(request)
                : new IdempotentRequestResponseWrapper(null);
        wrapper.setPayloadHash(payloadHash);
        return wrapper;
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
     * Stores the final response for {@code idempotencyKey}.
     *
     * <p>Carries the {@code payloadHash} written by {@code tryAcquire} forward onto the
     * new stored value (Story 3.6): without this, the hash used to detect payload
     * collisions would be lost the moment a call finishes, and a later call with a
     * different payload would incorrectly be treated as a plain cache hit instead of
     * {@code Lease#isMismatch()}. Known risk flagged in Story 3.5's Completion Notes,
     * fixed here since Story 3.6 is the first story that depends on it.</p>
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
                IdempotentRequestResponseWrapper newValue = prepareValue(request, response);
                newValue.setPayloadHash(requestResponseWrapper.getPayloadHash());
                this.valueOperations.set(idempotencyKey.getKeyValue(), newValue, ttl, timeUnit);
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

