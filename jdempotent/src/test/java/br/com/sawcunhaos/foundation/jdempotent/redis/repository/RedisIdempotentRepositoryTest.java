
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


import br.com.sawcunhaos.foundation.jdempotent.core.metrics.IdempotencyMetrics;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.Lease;
import br.com.sawcunhaos.foundation.jdempotent.redis.configuration.ScosJdempotentRedisProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RedisIdempotentRepositoryTest {


    @InjectMocks
    private RedisIdempotentRepository redisIdempotentRepository;

    @Mock
    private RedisTemplate redisTemplate;

    @Mock
    private ScosJdempotentRedisProperties scosJdempotentRedisProperties;

    @Mock
    private ValueOperations<String, IdempotentRequestResponseWrapper> valueOperations;

    @Captor
    private ArgumentCaptor<IdempotentRequestResponseWrapper> captor;

    @BeforeEach
    public void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisIdempotentRepository = new RedisIdempotentRepository(redisTemplate,
                scosJdempotentRedisProperties);
    }

    @Test
    public void given_an_available_object_when_redis_contains_then_return_true() {
        //Given
        IdempotencyKey idempotencyKey = new IdempotencyKey("key");
        var key = new IdempotencyKey("key");
        var wrapper = new IdempotentRequestResponseWrapper(
                new IdempotentRequestWrapper(new Object()));
        when(valueOperations.get(key.getKeyValue())).thenReturn(wrapper);

        //When
        Boolean isContain = redisIdempotentRepository.contains(idempotencyKey);

        //Then
        verify(valueOperations, times(1)).get(idempotencyKey.getKeyValue());
        assertTrue(isContain);
    }

    @Test
    public void given_an_unavailable_object_when_redis_contains_then_return_false() {
        //Given
        IdempotencyKey idempotencyKey = new IdempotencyKey("key1");

        //When
        Boolean isContain = redisIdempotentRepository.contains(idempotencyKey);

        //Then
        verify(valueOperations, times(1)).get(idempotencyKey.getKeyValue());
        assertFalse(isContain);
    }

    @Test
    public void given_an_available_object_when_get_response_then_return_response() {
        //Given
        var key = new IdempotencyKey("key");
        var wrapper = new IdempotentRequestResponseWrapper(
                new IdempotentRequestWrapper(new Object()));
        when(valueOperations.get(key.getKeyValue())).thenReturn(wrapper);
        var t = mock(IdempotentRequestResponseWrapper.class);

        IdempotentResponseWrapper expected = new IdempotentResponseWrapper("testt");
        when(t.getResponse()).thenReturn(expected);
        when(valueOperations.get(key.getKeyValue())).thenReturn(t);

        //When
        IdempotentResponseWrapper response = redisIdempotentRepository.getResponse(key);

        //Then
        verify(t).getResponse();
        assertEquals(response.getResponse(), "testt");
    }

    @Test
    public void given_idempotency_key_and_request_object_when_store_then_set_value_to_redis() {
        //Given
        IdempotencyKey key = new IdempotencyKey("key");
        IdempotentRequestWrapper request = new IdempotentRequestWrapper(123L);
        when(scosJdempotentRedisProperties.getPersistReqRes()).thenReturn(true);

        //When
        redisIdempotentRepository.store(key, request, 1L, TimeUnit.HOURS);

        //Then
        var argumentCaptor = ArgumentCaptor.forClass(IdempotentRequestResponseWrapper.class);
        verify(valueOperations).set(eq(key.getKeyValue()), argumentCaptor.capture(), eq(1L), eq(TimeUnit.HOURS));
        IdempotentRequestResponseWrapper value = argumentCaptor.getValue();
        assertEquals(value.getRequest().getRequest(), Collections.singletonList(123L));
    }

    @Test
    public void given_ttl_zero_when_store_then_set_value_to_redis_with_property_ttl() {
        //Given
        IdempotencyKey key = new IdempotencyKey("key");
        IdempotentRequestWrapper request = new IdempotentRequestWrapper(123L);
        when(scosJdempotentRedisProperties.getExpirationTimeHour()).thenReturn(99L);

        //When
        redisIdempotentRepository.store(key, request, 0L, TimeUnit.HOURS);

        //Then
        verify(valueOperations).set(eq(key.getKeyValue()), any(), eq(99L), eq(TimeUnit.HOURS));
    }

    @Test
    public void given_idempotency_key_when_remove_then_delete_redis_key() {
        //Given
        IdempotencyKey key = new IdempotencyKey("key");

        //When
        redisIdempotentRepository.remove(key);

        //Then
        verify(redisTemplate).delete(eq(key.getKeyValue()));
    }

    @Test
    public void given_idempotency_key_and_request_and_response_objects_when_set_response_then_set_response_to_key() {
        //Given
        IdempotencyKey key = new IdempotencyKey("key");
        IdempotentRequestWrapper request = new IdempotentRequestWrapper(123L);
        IdempotentResponseWrapper response = new IdempotentResponseWrapper("response");
        var wrapper = new IdempotentRequestResponseWrapper(
                new IdempotentRequestWrapper(new Object()));
        when(valueOperations.get(key.getKeyValue())).thenReturn(wrapper);
        assertNull(wrapper.getResponse());
        when(scosJdempotentRedisProperties.getPersistReqRes()).thenReturn(true);

        //When
        redisIdempotentRepository.setResponse(key, request, response, 1L, TimeUnit.HOURS);

        //Then
        var argumentCaptor = ArgumentCaptor.forClass(IdempotentRequestResponseWrapper.class);
        verify(valueOperations).set(eq(key.getKeyValue()), argumentCaptor.capture(), eq(1L), eq(TimeUnit.HOURS));
        IdempotentRequestResponseWrapper value = argumentCaptor.getValue();
        assertEquals(value.getRequest().getRequest(), Collections.singletonList(123L));
        assertEquals(value.getResponse().getResponse(), "response");
        assertEquals(wrapper.getResponse().getResponse(), "response");
    }

    @Test
    public void given_the_idempotence_key_and_the_request_and_response_objects_when_defining_the_response_one_must_save_the_key_without_the_request_and_response_object() {
        //Given
        IdempotencyKey key = new IdempotencyKey("key");
        IdempotentRequestWrapper request = new IdempotentRequestWrapper(123L);
        IdempotentResponseWrapper response = new IdempotentResponseWrapper("response");
        var wrapper = new IdempotentRequestResponseWrapper(
                new IdempotentRequestWrapper(new Object()));
        when(valueOperations.get(key.getKeyValue())).thenReturn(wrapper);
        assertNull(wrapper.getResponse());
        when(scosJdempotentRedisProperties.getPersistReqRes()).thenReturn(false);

        //When
        redisIdempotentRepository.setResponse(key, request, response, 1L, TimeUnit.HOURS);

        //Then
        var argumentCaptor = ArgumentCaptor.forClass(IdempotentRequestResponseWrapper.class);
        verify(valueOperations).set(eq(key.getKeyValue()), argumentCaptor.capture(), eq(1L), eq(TimeUnit.HOURS));
        IdempotentRequestResponseWrapper value = argumentCaptor.getValue();
        assertNull(value.getRequest());
        assertNull(value.getResponse());
        assertEquals(wrapper.getResponse().getResponse(), "response");
    }

    @Test
    public void given_a_different_payload_hash_already_stored_under_the_key_when_tryAcquire_then_lease_is_mismatch() {
        //Given
        IdempotencyKey key = new IdempotencyKey("key");
        var existing = new IdempotentRequestResponseWrapper(null, "other-hash");
        when(valueOperations.setIfAbsent(eq(key.getKeyValue()), any(), any(Duration.class))).thenReturn(false);
        when(valueOperations.get(key.getKeyValue())).thenReturn(existing);

        //When
        Lease lease = redisIdempotentRepository.tryAcquire(key, "new-hash", Duration.ofSeconds(30));

        //Then
        assertFalse(lease.isAcquired());
        assertTrue(lease.isMismatch());
        assertEquals("other-hash", lease.getExistingPayloadHash());
    }

    @Test
    public void given_the_same_payload_hash_already_stored_under_the_key_when_tryAcquire_then_lease_is_in_progress_not_mismatch() {
        //Given
        IdempotencyKey key = new IdempotencyKey("key");
        var existing = new IdempotentRequestResponseWrapper(null, "same-hash");
        when(valueOperations.setIfAbsent(eq(key.getKeyValue()), any(), any(Duration.class))).thenReturn(false);
        when(valueOperations.get(key.getKeyValue())).thenReturn(existing);

        //When
        Lease lease = redisIdempotentRepository.tryAcquire(key, "same-hash", Duration.ofSeconds(30));

        //Then
        assertFalse(lease.isAcquired());
        assertFalse(lease.isMismatch());
        assertFalse(lease.hasCachedResponse(), "no response was stored yet, this is a genuine in-progress call");
        assertNull(lease.getExistingResponse());
    }

    @Test
    public void given_the_same_payload_hash_already_finished_with_a_cached_response_when_tryAcquire_then_lease_is_not_mismatch_and_exposes_the_cached_response() {
        //Given
        IdempotencyKey key = new IdempotencyKey("key");
        IdempotentResponseWrapper cachedResponse = new IdempotentResponseWrapper("cached-result");
        var existing = new IdempotentRequestResponseWrapper(null, "same-hash");
        existing.setResponse(cachedResponse);
        when(valueOperations.setIfAbsent(eq(key.getKeyValue()), any(), any(Duration.class))).thenReturn(false);
        when(valueOperations.get(key.getKeyValue())).thenReturn(existing);

        //When
        Lease lease = redisIdempotentRepository.tryAcquire(key, "same-hash", Duration.ofSeconds(30));

        //Then
        assertFalse(lease.isAcquired());
        assertFalse(lease.isMismatch());
        assertTrue(lease.hasCachedResponse());
        // assertSame, not assertEquals: IdempotentResponseWrapper#equals(Object) compares
        // response.equals(obj) instead of obj.response — comparing a String against a
        // wrapper always returns false, a pre-existing bug unrelated to this story/test.
        assertSame(cachedResponse, lease.getExistingResponse());
    }

    @Test
    public void given_idempotency_key_when_set_response_then_payload_hash_is_carried_forward_to_the_new_stored_value() {
        // Story 3.6: without this, the hash used to detect payload collisions is lost the
        // moment a call finishes, and a later different-payload call would be treated as a
        // cache hit instead of a mismatch.
        //Given
        IdempotencyKey key = new IdempotencyKey("key");
        IdempotentRequestWrapper request = new IdempotentRequestWrapper(123L);
        IdempotentResponseWrapper response = new IdempotentResponseWrapper("response");
        var wrapper = new IdempotentRequestResponseWrapper(
                new IdempotentRequestWrapper(new Object()), "original-hash");
        when(valueOperations.get(key.getKeyValue())).thenReturn(wrapper);
        when(scosJdempotentRedisProperties.getPersistReqRes()).thenReturn(true);

        //When
        redisIdempotentRepository.setResponse(key, request, response, 1L, TimeUnit.HOURS);

        //Then
        var argumentCaptor = ArgumentCaptor.forClass(IdempotentRequestResponseWrapper.class);
        verify(valueOperations).set(eq(key.getKeyValue()), argumentCaptor.capture(), eq(1L), eq(TimeUnit.HOURS));
        assertEquals("original-hash", argumentCaptor.getValue().getPayloadHash());
    }

    @Test
    public void given_a_cache_miss_when_get_response_then_return_null_instead_of_throwing() {
        // Story 3.7 code review finding: a missing/expired key must be a plain null return, not
        // an NPE inside circuitBreaker.executeSupplier() — an NPE there would be wrongly counted
        // as a circuit breaker failure instead of the normal outcome a cache miss actually is.
        IdempotencyKey key = new IdempotencyKey("absent-key");
        when(valueOperations.get(key.getKeyValue())).thenReturn(null);

        assertNull(redisIdempotentRepository.getResponse(key));
    }

    // -----------------------------------------------------------------
    // resolveSlowCallThreshold(RedisTemplate) — private static, invoked via reflection since it
    // has no other externally observable seam (the circuit breaker built from it is itself
    // private). Story 3.7 code review findings: must resolve the real Lettuce commandTimeout when
    // available and valid, and must fall back to DEFAULT_SLOW_CALL_THRESHOLD (5s) whenever that
    // value would be unusable (non-Lettuce factory, null, zero, or negative).
    // -----------------------------------------------------------------

    private static Duration resolveSlowCallThreshold(RedisTemplate redisTemplate) throws Exception {
        Method method = RedisIdempotentRepository.class.getDeclaredMethod("resolveSlowCallThreshold", RedisTemplate.class);
        method.setAccessible(true);
        return (Duration) method.invoke(null, redisTemplate);
    }

    @Test
    public void given_a_lettuce_factory_with_a_positive_command_timeout_when_resolving_the_slow_call_threshold_then_that_timeout_is_used() throws Exception {
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(3))
                .build();
        LettuceConnectionFactory lettuceConnectionFactory = mock(LettuceConnectionFactory.class);
        when(lettuceConnectionFactory.getClientConfiguration()).thenReturn(clientConfiguration);
        RedisTemplate template = mock(RedisTemplate.class);
        when(template.getConnectionFactory()).thenReturn(lettuceConnectionFactory);

        assertEquals(Duration.ofSeconds(3), resolveSlowCallThreshold(template));
    }

    static Stream<Duration> invalidOrUnusableTimeouts() {
        return Stream.of(Duration.ZERO, Duration.ofSeconds(-1), null);
    }

    @ParameterizedTest
    @MethodSource("invalidOrUnusableTimeouts")
    public void given_a_lettuce_factory_with_a_zero_negative_or_null_command_timeout_when_resolving_the_slow_call_threshold_then_the_default_is_used(Duration invalidTimeout) throws Exception {
        LettuceClientConfiguration clientConfiguration = mock(LettuceClientConfiguration.class);
        when(clientConfiguration.getCommandTimeout()).thenReturn(invalidTimeout);
        LettuceConnectionFactory lettuceConnectionFactory = mock(LettuceConnectionFactory.class);
        when(lettuceConnectionFactory.getClientConfiguration()).thenReturn(clientConfiguration);
        RedisTemplate template = mock(RedisTemplate.class);
        when(template.getConnectionFactory()).thenReturn(lettuceConnectionFactory);

        assertEquals(Duration.ofSeconds(5), resolveSlowCallThreshold(template));
    }

    @Test
    public void given_a_non_lettuce_connection_factory_when_resolving_the_slow_call_threshold_then_the_default_is_used() throws Exception {
        RedisConnectionFactory nonLettuceFactory = mock(RedisConnectionFactory.class);
        RedisTemplate template = mock(RedisTemplate.class);
        when(template.getConnectionFactory()).thenReturn(nonLettuceFactory);

        assertEquals(Duration.ofSeconds(5), resolveSlowCallThreshold(template));
    }

    @Test
    public void given_a_null_connection_factory_when_resolving_the_slow_call_threshold_then_the_default_is_used() throws Exception {
        RedisTemplate template = mock(RedisTemplate.class);
        when(template.getConnectionFactory()).thenReturn(null);

        assertEquals(Duration.ofSeconds(5), resolveSlowCallThreshold(template));
    }

    /**
     * Story 3.11: {@code idempotency.backend_error} on every failed Redis operation, and
     * {@code idempotency.degraded}/{@code idempotency.degraded.transitions} when the real
     * (not mocked) circuit breaker actually flips state. No Docker/Testcontainers needed —
     * the breaker's failure counting and state machine are pure in-memory logic once
     * {@code valueOperations} is stubbed to always throw; the same
     * {@code minimumNumberOfCalls(5)} / default 50% failure-rate-threshold setup already
     * proven in {@code RedisIdempotentRepositoryFailOpenITTest} (Story 3.7) applies here.
     */
    @Test
    public void given_repeated_redis_failures_when_the_circuit_breaker_opens_then_backend_error_and_degraded_metrics_are_emitted() {
        IdempotencyMetrics metrics = mock(IdempotencyMetrics.class);
        RedisIdempotentRepository repository = new RedisIdempotentRepository(redisTemplate, scosJdempotentRedisProperties, metrics);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("boom"));

        // minimumNumberOfCalls(5): all 5 calls are genuinely attempted (breaker still CLOSED
        // throughout), each one fails and falls into the fail-open catch below.
        for (int i = 0; i < 5; i++) {
            assertFalse(repository.contains(new IdempotencyKey("key-" + i)));
        }

        verify(metrics, times(5)).backendError();
        // Default failure-rate-threshold (50%) with a 100% failure rate over the 5 calls above
        // trips the breaker CLOSED -> OPEN exactly once: one flip into "degraded", not one call
        // per check (Task 3: "não a cada verificação").
        verify(metrics, times(1)).degraded(true);
        verify(metrics, times(1)).degradedTransition();
        verify(metrics, times(0)).degraded(false);
    }

    // -----------------------------------------------------------------
    // Story 3.11 review finding #4: backend_error was only ever verified against a mock
    // IdempotencyMetrics for contains() (test above) — the other 5 Redis operations never were.
    // -----------------------------------------------------------------

    @Test
    public void given_redis_get_throws_when_getResponse_called_then_backend_error_metric_emitted() {
        IdempotencyMetrics metrics = mock(IdempotencyMetrics.class);
        RedisIdempotentRepository repository = new RedisIdempotentRepository(redisTemplate, scosJdempotentRedisProperties, metrics);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("boom"));

        assertNull(repository.getResponse(new IdempotencyKey("key")));

        verify(metrics, times(1)).backendError();
    }

    @Test
    public void given_redis_set_throws_when_store_called_then_backend_error_metric_emitted() {
        IdempotencyMetrics metrics = mock(IdempotencyMetrics.class);
        RedisIdempotentRepository repository = new RedisIdempotentRepository(redisTemplate, scosJdempotentRedisProperties, metrics);
        when(scosJdempotentRedisProperties.getPersistReqRes()).thenReturn(true);
        doThrow(new RuntimeException("boom")).when(valueOperations).set(any(), any(), anyLong(), any());

        repository.store(new IdempotencyKey("key"), new IdempotentRequestWrapper(123L), 1L, TimeUnit.HOURS);

        verify(metrics, times(1)).backendError();
    }

    @Test
    public void given_redis_setIfAbsent_throws_when_tryAcquire_called_then_backend_error_metric_emitted_and_fail_open() {
        IdempotencyMetrics metrics = mock(IdempotencyMetrics.class);
        RedisIdempotentRepository repository = new RedisIdempotentRepository(redisTemplate, scosJdempotentRedisProperties, metrics);
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class))).thenThrow(new RuntimeException("boom"));

        Lease lease = repository.tryAcquire(new IdempotencyKey("key"), "hash", Duration.ofSeconds(30));

        assertTrue(lease.isAcquired(), "fail-open: a Redis failure must never block the business request");
        verify(metrics, times(1)).backendError();
    }

    @Test
    public void given_redis_delete_throws_when_remove_called_then_backend_error_metric_emitted() {
        IdempotencyMetrics metrics = mock(IdempotencyMetrics.class);
        RedisIdempotentRepository repository = new RedisIdempotentRepository(redisTemplate, scosJdempotentRedisProperties, metrics);
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("boom"));

        repository.remove(new IdempotencyKey("key"));

        verify(metrics, times(1)).backendError();
    }

    @Test
    public void given_redis_get_throws_when_setResponse_called_then_backend_error_metric_emitted() {
        IdempotencyMetrics metrics = mock(IdempotencyMetrics.class);
        RedisIdempotentRepository repository = new RedisIdempotentRepository(redisTemplate, scosJdempotentRedisProperties, metrics);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("boom"));

        repository.setResponse(new IdempotencyKey("key"), null, new IdempotentResponseWrapper("r"), 1L, TimeUnit.HOURS);

        verify(metrics, times(1)).backendError();
    }

    /**
     * Story 3.11 review finding #5: the only existing transition test forces CLOSED -> OPEN and
     * checks {@code degraded(true)}/{@code degradedTransition()} fire once each — nothing proved
     * the "collapse redundant transitions" behavior the code comment/CHANGELOG describe: OPEN ->
     * HALF_OPEN (still degraded, must NOT re-fire) and HALF_OPEN -> CLOSED (recovery, must fire
     * {@code degraded(false)}/{@code degradedTransition()} exactly once). Uses the real circuit
     * breaker's {@code transitionToXState()} test hooks directly — same technique already used by
     * {@code RedisIdempotentRepositoryFailOpenITTest} (Story 3.7) — no Redis calls needed, this is
     * purely the breaker's own state machine.
     */
    @Test
    public void given_the_breaker_transitions_open_to_half_open_to_closed_then_only_the_actual_flips_emit_degraded_events() {
        IdempotencyMetrics metrics = mock(IdempotencyMetrics.class);
        RedisIdempotentRepository repository = new RedisIdempotentRepository(redisTemplate, scosJdempotentRedisProperties, metrics);
        CircuitBreaker circuitBreaker = (CircuitBreaker) ReflectionTestUtils.getField(repository, "circuitBreaker");

        circuitBreaker.transitionToOpenState();
        verify(metrics, times(1)).degraded(true);
        verify(metrics, times(1)).degradedTransition();

        // OPEN -> HALF_OPEN: both count as "degraded" (state != CLOSED) — the observable boolean
        // does not flip, so neither degraded(...) nor degradedTransition() may fire again.
        circuitBreaker.transitionToHalfOpenState();
        verify(metrics, times(1)).degraded(true);
        verify(metrics, times(1)).degradedTransition();
        verify(metrics, times(0)).degraded(false);

        // HALF_OPEN -> CLOSED: recovery, the boolean flips back to false — must fire exactly once.
        circuitBreaker.transitionToClosedState();
        verify(metrics, times(1)).degraded(false);
        verify(metrics, times(2)).degradedTransition();
    }
}
