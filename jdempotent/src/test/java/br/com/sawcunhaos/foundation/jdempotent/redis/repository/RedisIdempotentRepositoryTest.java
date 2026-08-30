
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


import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.Lease;
import br.com.sawcunhaos.foundation.jdempotent.redis.configuration.ScosJdempotentRedisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
}
