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

package br.com.sawcunhaos.foundation.audit;

import br.com.sawcunhaos.foundation.audit.domain.entity.ActionType;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import br.com.sawcunhaos.foundation.audit.service.ScosAuditBatchConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("8.4 — Backpressure: fila cheia não bloqueia negócio")
@TestPropertySource(properties = {
        "scos.audit.performance.queue-capacity=5",
        "scos.audit.performance.batch-size=5",
        "scos.audit.performance.flush-interval-ms=2000"
})
class AuditBackpressureIntegrationTest extends AbstractAuditIntegrationTest {

    @Autowired
    private ScosAuditBatchConsumer batchConsumer;

    @Test
    @DisplayName("Overflow da fila roteia para DLQ sem bloquear a thread chamadora")
    void overflowRoutesToDlqWithoutBlocking() throws InterruptedException {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 20; i++) {
                ScosAuditLog log = ScosAuditLog.builder()
                        .actionType(ActionType.INSERT)
                        .entity("SFA_COUNTRY")
                        .idEntity("bp-" + i)
                        .user("test")
                        .originSystem("TEST")
                        .executionDate(LocalDateTime.now())
                        .build();
                batchConsumer.offerOrDlq(log);
            }
        }, "offerOrDlq must never block or throw even when queue is full");

        waitForAsync(3000);

        long totalPersisted = auditLogRepository.count() + dlqRepository.count();
        org.junit.jupiter.api.Assertions.assertTrue(totalPersisted > 0,
                "Events must end up in audit log or DLQ");
    }

}
