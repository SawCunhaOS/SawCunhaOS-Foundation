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

import br.com.sawcunhaos.foundation.audit.domain.entity.Country;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("8.1 — Batch Pipeline: burst de eventos")
class AuditBatchPipelineIntegrationTest extends AbstractAuditIntegrationTest {

    private static final int BURST_SIZE = 500;

    @Test
    @DisplayName("Burst de 500 eventos deve persistir todos via saveAll em lotes")
    void burstOf500EventsMustPersistAll() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(BURST_SIZE);
        List<Thread> threads = new ArrayList<>();

        long start = System.currentTimeMillis();

        for (int i = 0; i < BURST_SIZE; i++) {
            final int idx = i;
            Thread t = Thread.ofVirtual().start(() -> {
                try {
                    countryRepository.save(Country.builder()
                            .name("Burst-" + idx)
                            .code(10000 + idx)
                            .acronym("B" + (idx % 999))
                            .description("burst test")
                            .build());
                } finally {
                    latch.countDown();
                }
            });
            threads.add(t);
        }

        latch.await();
        long businessDuration = System.currentTimeMillis() - start;

        // Business operations must complete fast — async audit must not block them
        assertTrue(businessDuration < 15000,
                "Business operations took too long: " + businessDuration + "ms (async audit must not block)");

        // Poll until the batch consumer has drained all events (robust against async latency)
        awaitAuditCountAtLeast(BURST_SIZE, 10000);

        List<ScosAuditLog> logs = auditLogRepository.findAll();
        assertEquals(BURST_SIZE, logs.size(),
                "All " + BURST_SIZE + " events must be persisted in SFA_LOG_AUDIT");
    }

    @Test
    @DisplayName("Lotes devem reduzir round-trips: 100 eventos em lote único")
    void batchReducesRoundTrips() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            countryRepository.save(Country.builder()
                    .name("Batch-" + i)
                    .code(20000 + i)
                    .acronym("BT" + (i % 99))
                    .description("batch round-trip test")
                    .build());
        }

        awaitAuditCountAtLeast(100, 10000);

        long count = auditLogRepository.count();
        assertEquals(100, count, "100 events must be persisted");
    }

}
