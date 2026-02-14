package br.com.sawcunhaos.foundation.jdempotent.core.aspect;

import br.com.sawcunhaos.foundation.jdempotent.core.constant.CryptographyAlgorithm;
import br.com.sawcunhaos.foundation.jdempotent.core.datasource.InMemoryIdempotentRepository;
import br.com.sawcunhaos.foundation.jdempotent.core.generator.DefaultKeyGenerator;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentIgnorableWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.IdempotentTestPayload;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.TestException;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.TestIdempotentResource;
import br.com.sawcunhaos.foundation.utils.annotation.jdempotent.JdempotentResource;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        IdempotentAspectITTest.class,
        TestAopContext.class,
        TestIdempotentResource.class,
        DefaultKeyGenerator.class,
        InMemoryIdempotentRepository.class
})
class IdempotentAspectITTest {

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

        IdempotencyKey idempotencyKey = defaultKeyGenerator.generateIdempotentKey(new IdempotentRequestWrapper(wrapper), "", new StringBuilder(), MessageDigest.getInstance(CryptographyAlgorithm.MD5.value()));

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

        IdempotencyKey idempotencyKey = defaultKeyGenerator.generateIdempotentKey(new IdempotentRequestWrapper(wrapper), "TestIdempotentResource", new StringBuilder(), MessageDigest.getInstance(CryptographyAlgorithm.MD5.value()));

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

        IdempotencyKey idempotencyKey = defaultKeyGenerator.generateIdempotentKey(new IdempotentRequestWrapper(wrapper), "TestIdempotentResource", new StringBuilder(), MessageDigest.getInstance(CryptographyAlgorithm.MD5.value()));

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
    void given_new_multiple_payloads_with_multiple_annotations_when_trigger_aspect_then_first_annotated_payload_that_will_be_available_in_repository() throws NoSuchAlgorithmException {
        //given
        IdempotentTestPayload test = new IdempotentTestPayload();
        IdempotentTestPayload test1 = new IdempotentTestPayload();
        Object test2 = new Object();
        IdempotentIgnorableWrapper wrapper = new IdempotentIgnorableWrapper();
        wrapper.getNonIgnoredFields().put("name", null);
        wrapper.getNonIgnoredFields().put("transactionId", null);
        IdempotencyKey idempotencyKey = defaultKeyGenerator.generateIdempotentKey(new IdempotentRequestWrapper(wrapper), "TestIdempotentResource", new StringBuilder(), MessageDigest.getInstance(CryptographyAlgorithm.MD5.value()));

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

        IdempotencyKey idempotencyKey = defaultKeyGenerator.generateIdempotentKey(new IdempotentRequestWrapper(wrapper), "", new StringBuilder(), MessageDigest.getInstance(CryptographyAlgorithm.MD5.value()));

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
        IdempotencyKey key = defaultKeyGenerator.generateIdempotentKey(new IdempotentRequestWrapper(wrapper), "", new StringBuilder(), MessageDigest.getInstance(CryptographyAlgorithm.MD5.value()));

        //when
        testIdempotentResource.idempotencyKeyAsString(idempotencyKey);

        //then
        assertTrue(idempotentRepository.contains(key));
    }

}
