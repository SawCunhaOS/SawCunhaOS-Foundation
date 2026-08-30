
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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
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

    /**
     * Fallback slow-call threshold used only when the Redis timeout cannot be resolved from
     * {@code redisTemplate}'s connection factory (e.g. a non-Lettuce factory, or a test double).
     * Mirrors the {@code commandTimeout} hardcoded in {@code ScosJdempotentRedisConfiguration}.
     */
    private static final Duration DEFAULT_SLOW_CALL_THRESHOLD = Duration.ofSeconds(5);

    private final ValueOperations<String, IdempotentRequestResponseWrapper> valueOperations;
    private final RedisTemplate redisTemplate;
    private final ScosJdempotentRedisProperties redisProperties;

    /**
     * Story 3.7 (AC #1, ADD-3): a single circuit breaker guards every Redis operation this
     * repository performs, so that once Redis is confirmed slow/down, subsequent calls
     * short-circuit immediately instead of each paying the full {@code spring.data.redis.timeout}
     * again — this is the "mechanical" fail-open the story adds on top of the try/catch fail-open
     * that already existed (see the catch blocks below, unchanged in spirit).
     *
     * <p>Ponytail (Dev Notes): intentionally ONE breaker per repository instance, not one per
     * operation (contains/store/setResponse/...) — extra granularity was not asked for and each
     * operation already fails open identically.</p>
     *
     * <p>Not Spring-AOP {@code @CircuitBreaker}: this repository is constructed with {@code new}
     * by {@code ScosJdempotentConfig} rather than resolved as a Spring bean, so no AOP proxy would
     * ever apply an annotation-driven breaker. Built programmatically instead, exactly as the
     * story's Task 3 allows ("ou CircuitBreakerRegistry programático").</p>
     */
    private final CircuitBreaker circuitBreaker;


    public RedisIdempotentRepository(@Qualifier("JdempotentRedisTemplate") RedisTemplate redisTemplate, ScosJdempotentRedisProperties redisProperties) {
        this.valueOperations = redisTemplate.opsForValue();
        this.redisTemplate = redisTemplate;
        this.redisProperties = redisProperties;
        this.circuitBreaker = CircuitBreaker.of("jdempotent-redis", CircuitBreakerConfig.custom()
                .slowCallDurationThreshold(resolveSlowCallThreshold(redisTemplate))
                .slowCallRateThreshold(100)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(1)
                .build());
    }

    /**
     * Resolves the Redis command timeout ({@code spring.data.redis.timeout}, as configured on the
     * Lettuce connection factory backing {@code redisTemplate}) so the breaker's
     * {@code slow-call-duration-threshold} treats a call as "slow" exactly when it has already
     * exceeded the timeout Redis calls are actually configured with (AC #1) — never an arbitrary,
     * disconnected value.
     */
    private static Duration resolveSlowCallThreshold(RedisTemplate redisTemplate) {
        if (redisTemplate.getConnectionFactory() instanceof LettuceConnectionFactory lettuceConnectionFactory) {
            Duration commandTimeout = lettuceConnectionFactory.getClientConfiguration().getCommandTimeout();
            // A zero/negative value is invalid for CircuitBreakerConfig.slowCallDurationThreshold
            // (IllegalArgumentException at bean construction, a bootstrap failure) — fall back
            // instead of ever passing it through.
            if (commandTimeout != null && !commandTimeout.isZero() && !commandTimeout.isNegative()) {
                return commandTimeout;
            }
        }
        return DEFAULT_SLOW_CALL_THRESHOLD;
    }

    @Override
    public boolean contains(IdempotencyKey idempotencyKey) {
        try {
            return circuitBreaker.executeSupplier(() -> this.valueOperations.get(idempotencyKey.getKeyValue()) != null);
        } catch (Exception e) {
            log.error("Error checking idempotency key in Redis: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public IdempotentResponseWrapper getResponse(IdempotencyKey idempotencyKey) {
        try {
            return circuitBreaker.executeSupplier(() -> {
                IdempotentRequestResponseWrapper stored = this.valueOperations.get(idempotencyKey.getKeyValue());
                // A legitimate cache miss (absent/expired key) must return null, not NPE inside
                // the breaker — an NPE here would be wrongly recorded as a circuit breaker
                // failure instead of the normal, expected outcome it actually is.
                return stored != null ? stored.getResponse() : null;
            });
        } catch (Exception e) {
            log.error("Error retrieving idempotent response from Redis: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void store(IdempotencyKey idempotencyKey, IdempotentRequestWrapper request, Long ttl, TimeUnit timeUnit) {
        try {
            Long effectiveTtl = ttl == 0 ? redisProperties.getExpirationTimeHour() : ttl;
            circuitBreaker.executeRunnable(() ->
                    this.valueOperations.set(idempotencyKey.getKeyValue(), prepareValue(request), effectiveTtl, timeUnit));
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
     *
     * <p><strong>Story 3.7, AC #4 (accepted risk):</strong> while the circuit breaker sits in
     * {@code HALF_OPEN} after a Redis outage, at most {@code permittedNumberOfCallsInHalfOpenState}
     * (1) concurrent caller is actually let through to test real Redis; any other concurrent
     * caller for the very same key is short-circuited by {@link io.github.resilience4j.circuitbreaker.CallNotPermittedException}
     * and falls into the {@code catch} below like any other Redis failure. If the trial call
     * genuinely reaches Redis and acquires the real lock while the short-circuited call
     * simultaneously fail-opens as "acquired" too, both callers can end up invoking the protected
     * business method concurrently for that one key — the same accepted risk documented for the
     * split-brain scenario (AC #3): the response is not reliably cached and the DB {@code UNIQUE}
     * constraint (NFR6) is the real guarantee against a duplicated side effect in that narrow
     * window, not this lock.</p>
     */
    @Override
    public Lease tryAcquire(IdempotencyKey idempotencyKey, String payloadHash, Duration ttl) {
        Duration effectiveTtl = (ttl == null || ttl.isZero() || ttl.isNegative())
                ? Duration.ofHours(redisProperties.getExpirationTimeHour())
                : ttl;
        try {
            return circuitBreaker.executeSupplier(() -> {
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
            });
        } catch (Exception e) {
            log.error("Error acquiring idempotent lease in Redis: {}", e.getMessage());
            // NOTE: fail-open on Redis errors (incl. the circuit breaker's own
            // CallNotPermittedException, Story 3.7), mirrors contains()/store() above which
            // already swallow exceptions and let the caller proceed rather than blocking on a
            // Redis outage.
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
            circuitBreaker.executeRunnable(() -> redisTemplate.delete(idempotencyKey.getKeyValue()));
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
     * <p><strong>Story 3.7, AC #3 (split-brain, accepted risk):</strong> when {@code tryAcquire}
     * acquired the lock while Redis was up but Redis then goes down (or times out past
     * {@code slow-call-duration-threshold}) before the protected method finishes, this call fails
     * open (the {@code catch} below) and the response is silently NOT cached. The client still
     * gets its successful response — the business request is never blocked by this — but a
     * subsequent retry will find no cached entry and re-execute the protected method from
     * scratch. This is a documented, accepted risk, not a bug to fix here: the real protection
     * against a duplicated side effect in that window is the database {@code UNIQUE} constraint on
     * the idempotency key (NFR6), which is the consumer's responsibility, not this module's.</p>
     *
     * @param idempotencyKey
     * @param request
     * @param response
     * @param ttl
     */
    @Override
    public void setResponse(IdempotencyKey idempotencyKey, IdempotentRequestWrapper request, IdempotentResponseWrapper response, Long ttl, TimeUnit timeUnit) {
        try {
            Long effectiveTtl = ttl == 0 ? redisProperties.getExpirationTimeHour() : ttl;
            // Single GET guarded by the breaker (not a nested contains() + a second GET as before):
            // avoids acquiring the circuit breaker's HALF_OPEN permit twice for what is logically
            // one operation, which would otherwise let a second, unrelated nested call spuriously
            // consume the one trial slot permittedNumberOfCallsInHalfOpenState allows (Story 3.7,
            // AC #4). Same null-check semantics as the previous contains() + get() pair.
            circuitBreaker.executeRunnable(() -> {
                IdempotentRequestResponseWrapper requestResponseWrapper = valueOperations.get(idempotencyKey.getKeyValue());
                if (requestResponseWrapper != null) {
                    requestResponseWrapper.setResponse(response);
                    IdempotentRequestResponseWrapper newValue = prepareValue(request, response);
                    newValue.setPayloadHash(requestResponseWrapper.getPayloadHash());
                    this.valueOperations.set(idempotencyKey.getKeyValue(), newValue, effectiveTtl, timeUnit);
                }
            });
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

