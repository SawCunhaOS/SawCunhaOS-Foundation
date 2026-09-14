
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
import br.com.sawcunhaos.foundation.jdempotent.core.exception.IdempotentPayloadMismatchException;
import br.com.sawcunhaos.foundation.jdempotent.core.exception.IdempotentReplayedFailureException;
import br.com.sawcunhaos.foundation.jdempotent.core.generator.DefaultKeyGenerator;
import br.com.sawcunhaos.foundation.jdempotent.core.generator.IdempotencyKeyResolver;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentIgnorableWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.IdempotentTestPayload;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.TestException;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.TestIdempotentResource;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.TestIdempotentResourceSubclass;
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
        TestIdempotentResourceSubclass.class,
        DefaultKeyGenerator.class,
        InMemoryIdempotentRepository.class
})
class IdempotentAspectTest {

    @Autowired
    private TestIdempotentResource testIdempotentResource;

    @Autowired
    private TestIdempotentResourceSubclass testIdempotentResourceSubclass;

    @Autowired
    private InMemoryIdempotentRepository idempotentRepository;

    @Autowired
    private DefaultKeyGenerator defaultKeyGenerator;


    // Story 3.20 debt: getDeclaredMethods()[1] indexed into reflection array order, which the
    // JVM never guarantees — flaky ~2/3 runs of the full module suite. Look up the specific
    // method by name instead.
    @Test
    void given_aop_context_then_run_with_aop_context() throws NoSuchMethodException {
        JdempotentResource jdempotentResource = TestIdempotentResource.class
                .getDeclaredMethod("idempotentMethod", IdempotentTestPayload.class)
                .getAnnotation(JdempotentResource.class);

        assertNotEquals(testIdempotentResource.getClass(), TestIdempotentResource.class);
        assertTrue
                (AopUtils.isAopProxy(testIdempotentResource));
        assertTrue(AopUtils.isCglibProxy(testIdempotentResource));
        assertNotNull(jdempotentResource, "TestIdempotentResource.idempotentMethod is no longer annotated with @JdempotentResource");

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
                IdempotentAspect.builder().build().getIdempotentNonIgnorableWrapper(List.of(test));

        IdempotencyKey idempotencyKey = new IdempotencyKeyResolver(defaultKeyGenerator)
                .resolve(new IdempotentRequestWrapper(wrapper), "");

        //when
        testIdempotentResource.idempotentMethod(test);

        //then
        assertTrue(idempotentRepository.contains(idempotencyKey));
    }

    @Test
    void given_idempotency_key_header_present_when_trigger_aspect_then_the_stored_key_is_header_derived() {
        // Story 3.13 (AC #1): keySource=HEADER_THEN_FIELDS wired all the way through the real
        // AOP-proxied IdempotentAspect.execute(), not just at the resolver level. Proven here
        // without hand-recomputing the expected hash: two calls sharing the same Idempotency-Key
        // header but DIFFERENT payload bodies must collide onto the very same stored key — if the
        // key were field-derived instead, the two different bodies would land under two
        // different keys and never collide at all. The collision is caught by the pre-existing,
        // key-source-agnostic Story 3.6 mismatch check (IdempotentPayloadMismatchException),
        // confirming this story reuses that mechanism rather than needing a new one (see
        // IdempotentKeyMismatchPolicy Javadoc / Story 3.13 Completion Notes).
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("Idempotency-Key", "client-supplied-key");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));
        try {
            IdempotentTestPayload first = new IdempotentTestPayload("first-body");
            IdempotentTestPayload second = new IdempotentTestPayload("second-body");

            //when: first call stores under the header-derived key
            testIdempotentResource.idempotentMethodWithHeaderKeySource(first);

            //then: a different payload body, same header, collides on the same key -> mismatch
            Assertions.assertThrows(
                    IdempotentPayloadMismatchException.class,
                    () -> testIdempotentResource.idempotentMethodWithHeaderKeySource(second)
            );
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    /**
     * Story 3.15 (Task 4, gap flagged in Story 3.19's 2026-08-30 review): the only test with a
     * custom TTL ({@code PrimeNumbersJdempotentEnableITTest}, {@code ttl=30, ttlTimeUnit=SECONDS})
     * only confirmed the key gets written, never waited for it to actually expire. This exercises
     * the real {@code @JdempotentResource} -> {@code IdempotentAspect.execute()} -> repository
     * path (not the repository in isolation) with a short TTL, proving the annotation's
     * configured TTL is genuinely honored end-to-end — including that the protected method
     * actually executes again afterward (the real gap: a test that only checks
     * {@code contains()}/{@code getResponse()} would pass even if a stale cached response were
     * silently replayed instead of a genuine re-execution).
     */
    @Test
    void given_short_ttl_when_ttl_configured_on_the_annotation_elapses_then_the_method_executes_again() throws InterruptedException, NoSuchAlgorithmException {
        //given
        IdempotentTestPayload test = new IdempotentTestPayload("short-ttl");
        IdempotentIgnorableWrapper wrapper = new IdempotentIgnorableWrapper();
        wrapper.getNonIgnoredFields().put("name", "short-ttl");
        wrapper.getNonIgnoredFields().put("transactionId", null);
        IdempotencyKey idempotencyKey = defaultKeyGenerator.generateIdempotentKey(new IdempotentRequestWrapper(wrapper), "TestIdempotentResource", new StringBuilder(), MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value()));

        //when: the annotated method runs through the real AOP-proxied aspect, which stores the
        //response with the annotation's ttl=200ms via setResponse()
        testIdempotentResource.idempotentMethodWithShortTtl(test);

        //then: present right away, executed exactly once
        assertTrue(idempotentRepository.contains(idempotencyKey));
        assertEquals(1, testIdempotentResource.getShortTtlInvocationCount());

        //when: the configured TTL elapses
        Thread.sleep(500);

        //then: the repository no longer honors the expired entry
        assertFalse(idempotentRepository.contains(idempotencyKey));
        assertNull(idempotentRepository.getResponse(idempotencyKey));

        //when: the same call is retried after the TTL elapsed
        testIdempotentResource.idempotentMethodWithShortTtl(test);

        //then: the method genuinely re-executed instead of replaying a stale cached response
        assertEquals(2, testIdempotentResource.getShortTtlInvocationCount());
    }

    @Test
    void given_ignored_field_and_custom_named_property_together_when_trigger_aspect_then_hash_reflects_each_correctly() throws NoSuchAlgorithmException {
        // Story 3.20 (AC #3): @JdempotentIgnore (age) and @JdempotentProperty (eventId, custom
        // key "transactionId") on different fields of the same object, validated end to end
        // through the real IdempotentAspect -- not just via the isolated chain-level tests
        // JdempotentIgnoreAnnotationChainTest/JdempotentPropertyAnnotationChainTest already cover
        // separately. The ignored field must never affect the composed key/hash; the property
        // field, under its custom name, must.
        //given
        IdempotentTestPayload sameEventFirstAge = new IdempotentTestPayload("combo");
        sameEventFirstAge.setEventId(42L);
        sameEventFirstAge.setAge(10L);

        IdempotentTestPayload sameEventDifferentAge = new IdempotentTestPayload("combo");
        sameEventDifferentAge.setEventId(42L);
        sameEventDifferentAge.setAge(999L);

        IdempotentTestPayload differentEventSameAge = new IdempotentTestPayload("combo");
        differentEventSameAge.setEventId(7L);
        differentEventSameAge.setAge(10L);

        IdempotentIgnorableWrapper wrapper = new IdempotentIgnorableWrapper();
        wrapper.getNonIgnoredFields().put("name", "combo");
        wrapper.getNonIgnoredFields().put("transactionId", 42L);
        IdempotencyKey expectedKey = defaultKeyGenerator.generateIdempotentKey(
                new IdempotentRequestWrapper(wrapper), "TestIdempotentResource", new StringBuilder(),
                MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value()));

        //when: first call stores under the key derived only from name+transactionId
        testIdempotentResource.idempotentMethodWithThreeParameter(sameEventFirstAge, sameEventFirstAge, sameEventFirstAge);
        assertTrue(idempotentRepository.contains(expectedKey));

        //then: a different @JdempotentIgnore value (age) on an otherwise identical payload does
        //not change the key at all -- both the idempotency key and the payload hash used for the
        //mismatch check are derived from the same ignored-field-free composition, so this
        //replays as a genuine duplicate instead of throwing IdempotentPayloadMismatchException
        Assertions.assertDoesNotThrow(() ->
                testIdempotentResource.idempotentMethodWithThreeParameter(sameEventDifferentAge, sameEventDifferentAge, sameEventDifferentAge));

        //then: a different @JdempotentProperty value (eventId/"transactionId"), by contrast,
        //composes a genuinely different key
        IdempotentIgnorableWrapper otherWrapper = new IdempotentIgnorableWrapper();
        otherWrapper.getNonIgnoredFields().put("name", "combo");
        otherWrapper.getNonIgnoredFields().put("transactionId", 7L);
        IdempotencyKey otherExpectedKey = defaultKeyGenerator.generateIdempotentKey(
                new IdempotentRequestWrapper(otherWrapper), "TestIdempotentResource", new StringBuilder(),
                MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value()));
        assertFalse(idempotentRepository.contains(otherExpectedKey));

        testIdempotentResource.idempotentMethodWithThreeParameter(differentEventSameAge, differentEventSameAge, differentEventSameAge);
        assertTrue(idempotentRepository.contains(otherExpectedKey));
        assertNotEquals(expectedKey, otherExpectedKey);
    }

    @Test
    void given_jdempotent_resource_method_overridden_without_repeating_annotation_when_called_twice_then_aspect_does_not_activate() {
        // Story 3.20 (AC #5): @JdempotentResource is a method-level annotation -- Java does not
        // carry it over to an overriding method that doesn't repeat it (unlike a class-level
        // @Inherited annotation), so TestIdempotentResourceSubclass#idempotentMethod runs
        // directly, with no aspect interception at all.
        //given
        IdempotentTestPayload test = new IdempotentTestPayload("subclass-override");

        //when: called twice with the exact same payload
        testIdempotentResourceSubclass.idempotentMethod(test);
        testIdempotentResourceSubclass.idempotentMethod(test);

        //then: both calls actually ran the real method body -- had the aspect activated, the
        //second call would have short-circuited (cached response) instead of incrementing again
        assertEquals(2, testIdempotentResourceSubclass.getOverriddenMethodInvocationCount());
    }

    @Test
    void given_default_cache_prefix_when_key_is_composed_then_no_prefix_segment_is_added() throws NoSuchAlgorithmException {
        // Story 3.20 (AC #6): @JdempotentResource#cachePrefix() defaults to "". Confirms what
        // that blank default actually does to the generated key end to end, instead of only
        // reusing the same blank listenerName value production code already passes in (as the
        // pre-existing "given_new_payload_..." test above does, without asserting on the literal
        // shape of the result).
        //given
        IdempotentTestPayload test = new IdempotentTestPayload("default-prefix");
        IdempotentIgnorableWrapper wrapper = new IdempotentIgnorableWrapper();
        wrapper.getNonIgnoredFields().put("name", "default-prefix");
        wrapper.getNonIgnoredFields().put("transactionId", null);

        IdempotencyKey keyWithBlankPrefix = defaultKeyGenerator.generateIdempotentKey(
                new IdempotentRequestWrapper(wrapper), "", new StringBuilder(),
                MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value()));
        IdempotencyKey keyWithExplicitPrefix = defaultKeyGenerator.generateIdempotentKey(
                new IdempotentRequestWrapper(wrapper), "SomePrefix", new StringBuilder(),
                MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value()));

        //then: a blank cachePrefix contributes no separator/segment at all -- the key is exactly
        //the raw hex digest, distinguishable from a non-blank prefix which always prepends
        //"prefix-"
        assertFalse(keyWithBlankPrefix.getKeyValue().contains("-"));
        assertTrue(keyWithExplicitPrefix.getKeyValue().startsWith("SomePrefix-"));
        assertNotEquals(keyWithBlankPrefix.getKeyValue(), keyWithExplicitPrefix.getKeyValue());

        //when: the real annotated method (its @JdempotentResource has no explicit cachePrefix)
        //runs through the actual AOP-proxied aspect
        testIdempotentResource.idempotentMethod(test);

        //then: it is stored under exactly the blank-prefix key computed above, confirming the
        //annotation's default genuinely produces a prefix-less key in production
        assertTrue(idempotentRepository.contains(keyWithBlankPrefix));
    }

}
