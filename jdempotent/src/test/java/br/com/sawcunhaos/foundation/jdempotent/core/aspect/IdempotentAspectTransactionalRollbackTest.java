
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
import br.com.sawcunhaos.foundation.jdempotent.core.generator.DefaultKeyGenerator;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentIgnorableWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.IdempotentTestPayload;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.TestException;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.TestTransactionalIdempotentResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.Advisor;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Story 3.9 (AC #1): a {@code @Transactional} method that also carries
 * {@code @JdempotentResource} and throws a business exception must roll back
 * its transaction AND release the idempotency key — no orphaned key left in
 * the repository (Redis in production, {@link InMemoryIdempotentRepository}
 * here) that would block a legitimate retry until TTL expiry.
 *
 * <p>Uses a real Spring AOP proxy (both {@code IdempotentAspect} and Spring's
 * {@code TransactionInterceptor} advisors on the same bean, via
 * {@link TestAopTransactionalContext}) so the actual advisor ordering
 * ({@code @Order} on {@code IdempotentAspect}) is exercised, not simulated.
 *
 * <p>Observed while implementing this story: the business-exception assertion
 * below already passes without an explicit {@code @Order} on
 * {@code IdempotentAspect} too, because {@code IdempotentAspect.execute()}
 * wraps {@code pjp.proceed()} in a try/catch that removes the key on ANY
 * exception regardless of whether the transactional interceptor sits inside
 * or outside it in the proxy chain — for a business exception thrown
 * directly by the target method, the exception unwinds through that catch
 * either way. This is exactly the "works by accident, not deterministically"
 * case the story anticipates: the explicit {@code @Order} is still required
 * so the ordering doesn't depend on incidental bean-registration order. The
 * commit-fails-after-successful-return case below is where the ordering
 * actually matters and is what this test class was written to prove.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        IdempotentAspectTransactionalRollbackTest.class,
        TestAopTransactionalContext.class,
        TestTransactionalIdempotentResource.class,
        DefaultKeyGenerator.class,
        InMemoryIdempotentRepository.class
})
class IdempotentAspectTransactionalRollbackTest {

    @Autowired
    private TestTransactionalIdempotentResource transactionalResource;

    @Autowired
    private InMemoryIdempotentRepository idempotentRepository;

    @Autowired
    private DefaultKeyGenerator defaultKeyGenerator;

    @Autowired
    private RollbackTrackingTransactionManager transactionManager;

    @Test
    void given_transactional_method_throws_business_exception_when_rollback_happens_then_idempotency_key_is_not_orphaned() throws NoSuchAlgorithmException {
        //given
        IdempotentTestPayload test = new IdempotentTestPayload();
        test.setName("rollback");
        IdempotentIgnorableWrapper wrapper = new IdempotentIgnorableWrapper();
        wrapper.getNonIgnoredFields().put("name", "rollback");
        wrapper.getNonIgnoredFields().put("transactionId", null);
        IdempotencyKey idempotencyKey = defaultKeyGenerator.generateIdempotentKey(
                new IdempotentRequestWrapper(wrapper),
                "TestTransactionalIdempotentResource",
                new StringBuilder(),
                MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value()));

        //when
        Assertions.assertThrows(
                TestException.class,
                () -> transactionalResource.idempotentMethodThrowingBusinessExceptionUnderTransaction(test)
        );

        //then: the transaction actually rolled back (proves @Transactional ran and rolled back,
        //not just that the business exception was thrown) ...
        assertTrue(transactionManager.wasRolledBack());
        assertFalse(transactionManager.wasCommitted());
        //... and the idempotency key was not left orphaned after that rollback (AC #1)
        assertFalse(idempotentRepository.contains(idempotencyKey));
    }

    /**
     * The case the class Javadoc calls out as the one the ordering fix
     * actually matters for: the target method returns normally (no business
     * exception), but the transaction's commit fails afterwards. With
     * {@code IdempotentAspect} outside the transactional interceptor
     * ({@code @Order(HIGHEST_PRECEDENCE)}), the commit happens inside
     * {@code pjp.proceed()}, so a commit failure is caught by the aspect's
     * own try/catch and the key is released (default {@code RELEASE} policy)
     * instead of being left holding a stale "success" response for data that
     * was never actually committed.
     */
    @Test
    void given_transactional_method_returns_normally_when_commit_fails_then_idempotency_key_is_not_left_with_stale_success_response() throws NoSuchAlgorithmException {
        //given
        IdempotentTestPayload test = new IdempotentTestPayload();
        test.setName("commit-fail");
        IdempotentIgnorableWrapper wrapper = new IdempotentIgnorableWrapper();
        wrapper.getNonIgnoredFields().put("name", "commit-fail");
        wrapper.getNonIgnoredFields().put("transactionId", null);
        IdempotencyKey idempotencyKey = defaultKeyGenerator.generateIdempotentKey(
                new IdempotentRequestWrapper(wrapper),
                "TestTransactionalIdempotentResource",
                new StringBuilder(),
                MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value()));
        transactionManager.failNextCommit();

        //when
        Assertions.assertThrows(
                TransactionSystemException.class,
                () -> transactionalResource.idempotentMethodReturningNormallyUnderTransaction(test)
        );

        //then: the commit really failed (proves this exercises the post-return commit-failure
        //path, not the business-exception path already covered above) ...
        assertFalse(transactionManager.wasCommitted());
        //... and the idempotency key was not left holding a stale success response (the
        //specific case @Order on IdempotentAspect was added to close)
        assertFalse(idempotentRepository.contains(idempotencyKey));
    }

    /**
     * Direct assertion on the AOP advisor ordering itself (Task 2), since the
     * exception-path assertion above passes regardless of ordering (see class
     * Javadoc) and can't alone prove {@code @Order} placed
     * {@code IdempotentAspect} outside the transactional interceptor.
     *
     * <p>Advisors are identified by type/identity, not by {@code @Order}
     * value or class-name substring match: matching on order value alone
     * would silently compare the wrong indices if any other advisor ever
     * shared {@code Ordered.HIGHEST_PRECEDENCE}, and a bare substring check
     * on the class name could match zero or more than one advisor.
     */
    @Test
    void given_transactional_and_idempotent_advisors_when_proxy_is_built_then_idempotent_aspect_sits_outside_the_transaction_interceptor() {
        Advisor[] advisors = ((Advised) transactionalResource).getAdvisors();

        int idempotentAspectIndex = -1;
        int transactionAdvisorIndex = -1;
        for (int i = 0; i < advisors.length; i++) {
            if (advisors[i] instanceof PointcutAdvisor pointcutAdvisor
                    && pointcutAdvisor.getAdvice() instanceof AbstractAspectJAdvice aspectJAdvice
                    && aspectJAdvice.getAspectJAdviceMethod().getDeclaringClass().equals(IdempotentAspect.class)) {
                assertEquals(-1, idempotentAspectIndex, "Found more than one IdempotentAspect advisor in the proxy chain: " + Arrays.toString(advisors));
                idempotentAspectIndex = i;
            }
            if (advisors[i] instanceof BeanFactoryTransactionAttributeSourceAdvisor) {
                assertEquals(-1, transactionAdvisorIndex, "Found more than one transactional advisor in the proxy chain: " + Arrays.toString(advisors));
                transactionAdvisorIndex = i;
            }
        }

        assertTrue(idempotentAspectIndex >= 0, "IdempotentAspect advisor not found in proxy chain: " + Arrays.toString(advisors));
        assertTrue(transactionAdvisorIndex >= 0, "Transactional advisor not found in proxy chain: " + Arrays.toString(advisors));
        // Lower index = invoked first = outer. IdempotentAspect must wrap (be outside) the
        // transactional interceptor so its catch block only runs after the transaction has
        // already rolled back.
        assertTrue(idempotentAspectIndex < transactionAdvisorIndex,
                "Expected IdempotentAspect's advisor (index " + idempotentAspectIndex
                        + ") to sit outside (lower index than) the transactional advisor (index "
                        + transactionAdvisorIndex + "): " + Arrays.toString(advisors));
    }
}
