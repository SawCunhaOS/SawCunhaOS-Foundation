
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

package br.com.sawcunhaos.foundation.jdempotent.core.aspect;

import br.com.sawcunhaos.foundation.jdempotent.core.callback.ErrorConditionalCallback;
import br.com.sawcunhaos.foundation.jdempotent.core.datasource.IdempotentRepository;
import br.com.sawcunhaos.foundation.jdempotent.core.generator.DefaultKeyGenerator;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentIgnorableWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.IdempotentTestPayload;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.TestIdempotentResource;
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentResource;
import lombok.Data;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {
        TestIdempotentResource.class,
        IdempotentAspectITTest.class,
        TestAopContext.class
})
class IdempotentAspectUTTest {

    @InjectMocks
    private IdempotentAspect idempotentAspect;

    @Mock
    private IdempotentRepository idempotentRepository;

    @Mock
    private DefaultKeyGenerator defaultKeyGenerator;

    @Mock
    private ErrorConditionalCallback errorCallback;

    @Test
    void given_new_payload_when_key_not_in_repository_and_method_has_one_arg_then_should_store_repository() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = TestIdempotentResource.class.getMethod("idempotentMethod", IdempotentTestPayload.class);

        IdempotentTestPayload payload = new IdempotentTestPayload("payload");
        TestIdempotentResource testIdempotentResource = mock(TestIdempotentResource.class);

        when(defaultKeyGenerator.generateIdempotentKey(any(),any(),any(),any())).thenReturn(new IdempotencyKey("123"));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testIdempotentResource);
        when(joinPoint.getTarget().getClass().getSimpleName()).thenReturn("TestIdempotentResource");
        when(idempotentRepository.contains(any())).thenReturn(false);

        //when
        idempotentAspect.execute(joinPoint);

        //then
        verify(joinPoint, times(4)).getSignature();
        verify(signature, times(3)).getMethod();
        verify(joinPoint).getTarget();
        verify(idempotentRepository, times(1)).store(any(), any(), any(), any());
        verify(joinPoint).proceed();
        verify(idempotentRepository, times(1)).setResponse(any(), any(), any(), any(), any());
    }

    @Test
    void given_actual_payload_when_key_in_repository_and_method_has_one_arg_then_should_not_store_repository() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = TestIdempotentResource.class.getMethod("idempotentMethod", IdempotentTestPayload.class);
        JdempotentResource jdempotentResource = mock(JdempotentResource.class);
        IdempotentTestPayload payload = new IdempotentTestPayload("payload");
        TestIdempotentResource testIdempotentResource = mock(TestIdempotentResource.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testIdempotentResource);
        when(joinPoint.getTarget().getClass().getSimpleName()).thenReturn("TestIdempotentResource");
        when(idempotentRepository.contains(any())).thenReturn(true);

        //when
        idempotentAspect.execute(joinPoint);

        //then
        verify(joinPoint, times(4)).getSignature();
        verify(signature, times(3)).getMethod();
        verify(joinPoint).getTarget();
        verify(joinPoint, times(0)).proceed();
        verify(idempotentRepository, times(1)).getResponse(any());
    }

    @Test
    void given_actual_payload_when_key_in_repository_and_method_has_one_arg_then_should_store_repository_before_should_be_delete() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = TestIdempotentResource.class.getMethod("idempotentMethodThrowingARuntimeException", IdempotentTestPayload.class);
        IdempotentTestPayload payload = new IdempotentTestPayload("payload");
        TestIdempotentResource testIdempotentResource = mock(TestIdempotentResource.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testIdempotentResource);
        when(joinPoint.getTarget().getClass().getSimpleName()).thenReturn("TestIdempotentResource");
        when(idempotentRepository.contains(any())).thenReturn(false);

        //when
        Assertions.assertThrows(
                NullPointerException.class,
                () -> idempotentAspect.execute(joinPoint)
        );

        //then
        verify(joinPoint, times(4)).getSignature();
        verify(signature, times(3)).getMethod();
        verify(joinPoint).getTarget();
        verify(joinPoint, times(0)).proceed();
        verify(idempotentRepository, times(0)).remove(any());
        verify(idempotentRepository, times(0)).getResponse(any());
    }

    @Test
    void not_given_a_payload_then_return_exception() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        TestIdempotentResource testIdempotentResource = mock(TestIdempotentResource.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.getTarget()).thenReturn(testIdempotentResource);
        when(joinPoint.getTarget().getClass().getSimpleName()).thenReturn("TestIdempotentResource");

        //when
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> idempotentAspect.execute(joinPoint)
        );

        //then
        assertEquals("Idempotent method not found", illegalStateException.getMessage());
        verify(joinPoint).getTarget();
        verify(joinPoint).getSignature();
        verify(signature, times(0)).getMethod();
        verify(joinPoint, times(0)).proceed();
    }

    @Test
    void given_a_payload_when_called_error_callback_then_should_return_exception() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = TestIdempotentResource.class.getMethod("idempotentMethodThrowingARuntimeException", IdempotentTestPayload.class);
        IdempotentTestPayload payload = new IdempotentTestPayload("payload");
        TestIdempotentResource testIdempotentResource = mock(TestIdempotentResource.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testIdempotentResource);
        when(joinPoint.getTarget().getClass().getSimpleName()).thenReturn("TestIdempotentResource");
        when(idempotentRepository.contains(any())).thenReturn(false);

        Assertions.assertThrows(
                NullPointerException.class,
                () -> idempotentAspect.execute(joinPoint)
        );

        //then
        verify(joinPoint, times(4)).getSignature();
        verify(signature, times(3)).getMethod();
        verify(joinPoint).getTarget();
        verify(joinPoint, times(0)).proceed();
        verify(idempotentRepository, times(0)).remove(any());
    }

    @Test
    void given_a_payload_include_one_parameter_when_find_idempotent_request_then_return_idempotent_ignorable_wrapper() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        IdempotentTestPayload payload = new IdempotentTestPayload("payload");

        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});

        //when
        var idempotentRequestWrapper = idempotentAspect.findIdempotentRequestArg(joinPoint);

        //then
        List<Object> requestWrapperRequests = idempotentRequestWrapper.getRequest();
        assertEquals(requestWrapperRequests.size(), 1);
        IdempotentIgnorableWrapper requestWrapperRequest = (IdempotentIgnorableWrapper) requestWrapperRequests.get(0);
        assertEquals(requestWrapperRequest.getNonIgnoredFields().size(), 2);
        assertEquals(requestWrapperRequest.getNonIgnoredFields().get("name"), "payload");
        assertNull(requestWrapperRequest.getNonIgnoredFields().get("transactionId"));
        verify(joinPoint).getArgs();
    }

    @Test
    void given_a_payload_with_jdempotent_property_when_find_idempotent_request_then_return_idempotent_ignorable_wrapper() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        IdempotentTestPayload payload = new IdempotentTestPayload("payload");
        payload.setEventId(1l);

        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});

        //when
        var idempotentRequestWrapper = idempotentAspect.findIdempotentRequestArg(joinPoint);

        //then
        List<Object> requestWrapperRequests = idempotentRequestWrapper.getRequest();
        assertEquals(requestWrapperRequests.size(), 1);
        IdempotentIgnorableWrapper requestWrapperRequest = (IdempotentIgnorableWrapper) requestWrapperRequests.get(0);
        assertEquals(requestWrapperRequest.getNonIgnoredFields().size(), 2);
        assertEquals(requestWrapperRequest.getNonIgnoredFields().get("name"), "payload");
        assertEquals(requestWrapperRequest.getNonIgnoredFields().get("transactionId"), 1l);
        verify(joinPoint).getArgs();
    }

    @Test
    void given_a_payload_with_field_inherited_from_superclass_when_find_idempotent_request_then_key_includes_inherited_field() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        ChildPayload payload = new ChildPayload();
        payload.setName("payload");
        payload.setBaseField("baseValue");

        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});

        //when
        var idempotentRequestWrapper = idempotentAspect.findIdempotentRequestArg(joinPoint);

        //then
        List<Object> requestWrapperRequests = idempotentRequestWrapper.getRequest();
        assertEquals(requestWrapperRequests.size(), 1);
        IdempotentIgnorableWrapper requestWrapperRequest = (IdempotentIgnorableWrapper) requestWrapperRequests.get(0);
        assertEquals(requestWrapperRequest.getNonIgnoredFields().size(), 2);
        assertEquals(requestWrapperRequest.getNonIgnoredFields().get("name"), "payload");
        assertEquals(requestWrapperRequest.getNonIgnoredFields().get("baseField"), "baseValue");
        verify(joinPoint).getArgs();
    }

    @Test
    void given_a_payload_with_field_name_shadowed_from_superclass_when_find_idempotent_request_then_key_uses_subclass_value() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        ShadowingChildPayload payload = new ShadowingChildPayload();
        payload.setName("childValue");
        Field baseNameField = ShadowingBasePayload.class.getDeclaredField("name");
        baseNameField.setAccessible(true);
        baseNameField.set(payload, "baseValue");

        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});

        //when
        var idempotentRequestWrapper = idempotentAspect.findIdempotentRequestArg(joinPoint);

        //then
        List<Object> requestWrapperRequests = idempotentRequestWrapper.getRequest();
        assertEquals(requestWrapperRequests.size(), 1);
        IdempotentIgnorableWrapper requestWrapperRequest = (IdempotentIgnorableWrapper) requestWrapperRequests.get(0);
        assertEquals(requestWrapperRequest.getNonIgnoredFields().size(), 1);
        assertEquals(requestWrapperRequest.getNonIgnoredFields().get("name"), "childValue");
        verify(joinPoint).getArgs();
    }

    @Data
    private static class BasePayload {
        private String baseField;
    }

    @Data
    private static class ChildPayload extends BasePayload {
        private String name;
    }

    private static class ShadowingBasePayload {
        private String name;
    }

    @Data
    private static class ShadowingChildPayload extends ShadowingBasePayload {
        private String name;
    }
}
