
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

import br.com.sawcunhaos.foundation.jdempotent.core.constant.CryptographyAlgorithm;
import br.com.sawcunhaos.foundation.jdempotent.core.datasource.InMemoryIdempotentRepository;
import br.com.sawcunhaos.foundation.jdempotent.core.exception.IdempotentReplayedFailureException;
import br.com.sawcunhaos.foundation.jdempotent.core.generator.DefaultKeyGenerator;
import br.com.sawcunhaos.foundation.jdempotent.core.generator.IdempotencyKeyResolver;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentIgnorableWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.IdempotentTestPayload;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.TestException;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.TestIdempotentResource;
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        IdempotentAspectTest.class,
        TestAopContext.class,
        TestIdempotentResource.class,
        DefaultKeyGenerator.class,
        InMemoryIdempotentRepository.class
})
class IdempotentAspectTest {

    @Autowired
    private TestIdempotentResource testIdempotentResource;

    @Autowired
    private InMemoryIdempotentRepository idempotentRepository;

    @Autowired
    private DefaultKeyGenerator defaultKeyGenerator;


    @Test
    void given_aop_context_then_run_with_aop_context() {
        JdempotentResource jdempotentResource = TestIdempotentResource.class.getDeclaredMethods()[1].getAnnotation(JdempotentResource.class);

        assertNotEquals(testIdempotentResource.getClass(), TestIdempotentResource.class);
        assertTrue
                (AopUtils.isAopProxy(testIdempotentResource));
        assertTrue(AopUtils.isCglibProxy(testIdempotentResource));
        assertNotNull(jdempotentResource);

        assertEquals(AopProxyUtils.ultimateTargetClass(testIdempotentResource), TestIdempotentResource.class);
        assertEquals(AopTestUtils.getTargetObject(testIdempotentResource).getClass(), TestIdempotentResource.class);
        assertEquals(AopTestUtils.getUltimateTargetObject(testIdempotentResource).getClass(), TestIdempotentResource.class);
    }

    @Test
    void given_new_payload_when_trigger_aspect_then_that_will_be_aviable_in_repository() throws NoSuchAlgorithmException {
        //given
        IdempotentTestPayload test = new IdempotentTestPayload();
        IdempotentIgnorableWrapper wrapper = new IdempotentIgnorableWrapper();
        wrapper.getNonIgnoredFields().put("name", null);
        wrapper.getNonIgnoredFields().put("transactionId", null);

        IdempotencyKey idempotencyKey = defaultKeyGenerator.generateIdempotentKey(new IdempotentRequestWrapper(wrapper), "", new StringBuilder(), MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value()));

        //when
        testIdempotentResource.idempotentMethod(test);

        //then
        assertTrue(idempotentRepository.contains(idempotencyKey));
    }

    @Test
    void given_new_multiple_payloads_when_trigger_aspect_then_that_will_be_available_in_repository() throws NoSuchAlgorithmException {
        //given
        IdempotentTestPayload test = new IdempotentTestPayload();
        IdempotentTestPayload test1 = new IdempotentTestPayload();
        IdempotentTestPayload test2 = new IdempotentTestPayload();
        IdempotentIgnorableWrapper wrapper = new IdempotentIgnorableWrapper();
        wrapper.getNonIgnoredFields().put("name", null);
        wrapper.getNonIgnoredFields().put("transactionId", null);

        IdempotencyKey idempotencyKey = defaultKeyGenerator.generateIdempotentKey(new IdempotentRequestWrapper(wrapper), "TestIdempotentResource", new StringBuilder(), MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value()));

        //when
        testIdempotentResource.idempotentMethodWithThreeParameter(test, test1, test2);

        //then
        assertTrue(idempotentRepository.contains(idempotencyKey));
    }

    @Test
    void given_invalid_payload_when_trigger_aspect_then_throw_test_exception_and_repository_will_be_empty() throws NoSuchAlgorithmException {
        //given
        IdempotentTestPayload test = new IdempotentTestPayload();
        test.setName("invalid");
        IdempotentIgnorableWrapper wrapper = new IdempotentIgnorableWrapper();
        wrapper.getNonIgnoredFields().put("name", "invalid");

        IdempotencyKey idempotencyKey = defaultKeyGenerator.generateIdempotentKey(new IdempotentRequestWrapper(wrapper), "TestIdempotentResource", new StringBuilder(), MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value()));

        //when
        TestException illegalStateException = Assertions.assertThrows(
                TestException.class,
                () -> testIdempotentResource.idempotentMethodThrowingARuntimeException(test)
        );

        //then
        assertFalse(idempotentRepository.contains(idempotencyKey));
        assertNull(illegalStateException.getMessage());
    }

    @Test
    void given_keep_failed_policy_when_trigger_aspect_then_key_remains_and_failure_is_replayed_without_reexecution() throws NoSuchAlgorithmException {
        //given
        IdempotentTestPayload test = new IdempotentTestPayload();
        test.setName("keepFailed");
        IdempotentIgnorableWrapper wrapper = new IdempotentIgnorableWrapper();
        wrapper.getNonIgnoredFields().put("name", "keepFailed");
        wrapper.getNonIgnoredFields().put("transactionId", null);

        IdempotencyKey idempotencyKey = defaultKeyGenerator.generateIdempotentKey(new IdempotentRequestWrapper(wrapper), "TestIdempotentResource", new StringBuilder(), MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value()));

        //when: first call fails with a business exception
        TestException firstException = Assertions.assertThrows(
                TestException.class,
                () -> testIdempotentResource.idempotentMethodThrowingARuntimeExceptionKeepFailed(test)
        );

        //then: KEEP_FAILED keeps the key (AC #1), instead of removing it
        assertTrue(idempotentRepository.contains(idempotencyKey));
        assertEquals(1, testIdempotentResource.getKeepFailedInvocationCount());

        //when: a subsequent call with the same idempotency key
        IdempotentReplayedFailureException replayedException = Assertions.assertThrows(
                IdempotentReplayedFailureException.class,
                () -> testIdempotentResource.idempotentMethodThrowingARuntimeExceptionKeepFailed(test)
        );

        //then: the recorded failure is replayed (not the original exception instance, which the
        //repository may not be able to serialize/deserialize as-is), the method body did not run again (AC #1)
        assertNotEquals(TestException.class, replayedException.getClass());
        assertEquals(firstException.getClass().getName(), replayedException.getOriginalExceptionClassName());
        assertEquals(1, testIdempotentResource.getKeepFailedInvocationCount());
    }

    @Test
    void given_new_multiple_payloads_with_multiple_annotations_when_trigger_aspect_then_first_annotated_payload_that_will_be_available_in_repository() throws NoSuchAlgorithmException {
        //given
        IdempotentTestPayload test = new IdempotentTestPayload();
        IdempotentTestPayload test1 = new IdempotentTestPayload();
        Object test2 = new Object();
        IdempotentIgnorableWrapper wrapper = new IdempotentIgnorableWrapper();
        wrapper.getNonIgnoredFields().put("name", null);
        wrapper.getNonIgnoredFields().put("transactionId", null);
        IdempotencyKey idempotencyKey = defaultKeyGenerator.generateIdempotentKey(new IdempotentRequestWrapper(wrapper), "TestIdempotentResource", new StringBuilder(), MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value()));

        //when
        testIdempotentResource.idempotentMethodWithThreeParamaterAndMultipleJdempotentRequestPayloadAnnotation(test, test1, test2);

        //then
        assertTrue(idempotentRepository.contains(idempotencyKey));
    }

    @Test
    void given_no_args_when_trigger_aspect_then_throw_illegal_state_exception() {
        //given
        //when
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> testIdempotentResource.idempotentMethodWithZeroParamater()
        );

        //then
        assertEquals("Idempotent method not found", illegalStateException.getMessage());
    }

    @Test
    void given_multiple_args_without_idempotent_request_annotation_when_trigger_aspect_then_throw_illegal_state_exception() {
        //given
        IdempotentTestPayload test = new IdempotentTestPayload();
        IdempotentTestPayload test1 = new IdempotentTestPayload();

        //when
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> testIdempotentResource.methodWithTwoParamater(test, test1)
        );

        //then
        assertEquals("Idempotent method not found", illegalStateException.getMessage());
    }

    @Test
    void given_jdempotent_id_then_args_should_have_idempotency_id() throws NoSuchAlgorithmException {
        //given
        IdempotentTestPayload test = new IdempotentTestPayload();
        IdempotentIgnorableWrapper wrapper = new IdempotentIgnorableWrapper();
        wrapper.getNonIgnoredFields().put("name", null);
        wrapper.getNonIgnoredFields().put("transactionId", null);

        IdempotencyKey idempotencyKey = defaultKeyGenerator.generateIdempotentKey(new IdempotentRequestWrapper(wrapper), "", new StringBuilder(), MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value()));

        //when
        testIdempotentResource.idempotentMethod(test);

        //then
        assertTrue(idempotentRepository.contains(idempotencyKey));
    }

    @Test
    void given_new_payload_as_string_when_trigger_aspect_then_that_will_be_aviable_in_repository() throws NoSuchAlgorithmException {
        //given
        String idempotencyKey = "key";
        IdempotentTestPayload test = new IdempotentTestPayload();
        IdempotentIgnorableWrapper wrapper = new IdempotentIgnorableWrapper();
        wrapper.getNonIgnoredFields().put(idempotencyKey, idempotencyKey);
        IdempotencyKey key = defaultKeyGenerator.generateIdempotentKey(new IdempotentRequestWrapper(wrapper), "", new StringBuilder(), MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value()));

        //when
        testIdempotentResource.idempotencyKeyAsString(idempotencyKey);

        //then
        assertTrue(idempotentRepository.contains(key));
    }

    @Test
    void given_resolver_invoked_directly_without_aop_when_compared_to_the_aspect_flow_then_produces_the_same_key() throws IllegalAccessException {
        // Story 3.12 (Task 4, test 3): IdempotencyKeyResolver must be usable outside
        // IdempotentAspect/AOP (e.g. a future messaging entrypoint, Story 3.13) and still
        // agree with the key the real aspect flow computes and stores under — resolved here
        // via the same `defaultKeyGenerator` bean TestAopContext wires into the AOP-proxied
        // aspect (patch: a bare `new IdempotencyKeyResolver()` would only agree by coincidence,
        // since both default to a null namespace), and via the same field-collection path
        // (`getIdempotentNonIgnorableWrapper`) production code actually uses.
        //given
        IdempotentTestPayload test = new IdempotentTestPayload();
        test.setName("resolver-direct");
        IdempotentIgnorableWrapper wrapper =
                new IdempotentAspect().getIdempotentNonIgnorableWrapper(List.of(test));

        IdempotencyKey idempotencyKey = new IdempotencyKeyResolver(defaultKeyGenerator)
                .resolve(new IdempotentRequestWrapper(wrapper), "");

        //when
        testIdempotentResource.idempotentMethod(test);

        //then
        assertTrue(idempotentRepository.contains(idempotencyKey));
    }

}
