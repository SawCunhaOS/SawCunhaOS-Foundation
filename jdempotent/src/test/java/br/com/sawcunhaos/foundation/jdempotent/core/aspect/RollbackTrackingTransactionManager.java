
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

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Story 3.9: minimal {@code PlatformTransactionManager} fake for
 * {@code @Transactional} rollback tests. The jdempotent module has no
 * JDBC/JPA dependency (no real resource to begin/commit/rollback), so this
 * just tracks whether Spring's {@code TransactionInterceptor} called
 * {@link #doRollback} / {@link #doCommit} — enough to prove a real rollback
 * happened before {@code IdempotentAspect}'s catch block runs. {@link #failNextCommit()}
 * additionally simulates a commit that fails after the target method already
 * returned successfully.
 */
public class RollbackTrackingTransactionManager extends AbstractPlatformTransactionManager {

    private final AtomicBoolean rolledBack = new AtomicBoolean(false);
    private final AtomicBoolean committed = new AtomicBoolean(false);
    private final AtomicBoolean failOnCommit = new AtomicBoolean(false);

    @Override
    protected Object doGetTransaction() {
        return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        // no real resource to begin
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        if (failOnCommit.getAndSet(false)) {
            throw new TransactionSystemException("Simulated commit failure (Story 3.9 test)");
        }
        committed.set(true);
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        rolledBack.set(true);
    }

    public void failNextCommit() {
        failOnCommit.set(true);
    }

    public boolean wasRolledBack() {
        return rolledBack.get();
    }

    public boolean wasCommitted() {
        return committed.get();
    }
}
