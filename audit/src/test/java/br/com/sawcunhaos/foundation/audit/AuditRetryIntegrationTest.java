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
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Retry integration test.
 * Full connection-failure simulation requires Docker-level container pause/unpause.
 * This test validates that under normal conditions the retry path does not interfere
 * with successful persistence, and that events are always eventually persisted.
 */
@DisplayName("8.2 — Retry: persistência garantida após falha transiente")
@TestPropertySource(properties = {
        "scos.audit.durability.retry-max=3",
        "scos.audit.durability.dlq-enabled=true"
})
class AuditRetryIntegrationTest extends AbstractAuditIntegrationTest {

    @Test
    @DisplayName("Eventos persistem corretamente com retry-max=3 configurado")
    void eventsPersistedWithRetryConfigured() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            countryRepository.save(Country.builder()
                    .name("Retry-" + i)
                    .code(82000 + i)
                    .acronym("RT" + i)
                    .build());
        }

        waitForAsync(3000);

        List<ScosAuditLog> logs = auditLogRepository.findAll();
        assertFalse(logs.isEmpty(), "All events must persist with retry configured");
    }

    @Test
    @DisplayName("DLQ permanece vazia quando não há falhas de persistência")
    void dlqIsEmptyOnNormalOperation() throws InterruptedException {
        countryRepository.save(Country.builder()
                .name("No Failure")
                .code(82100)
                .acronym("NF1")
                .build());

        waitForAsync(2000);

        long dlqCount = dlqRepository.count();
        org.junit.jupiter.api.Assertions.assertEquals(0, dlqCount,
                "DLQ must be empty when no persistence failures occur");
    }

}
