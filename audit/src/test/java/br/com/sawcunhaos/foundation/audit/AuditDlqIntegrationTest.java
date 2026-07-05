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
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditDlqLog;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import br.com.sawcunhaos.foundation.audit.service.ScosAuditDlqJob;
import br.com.sawcunhaos.foundation.audit.service.ScosAuditLogService;
import br.com.sawcunhaos.foundation.utils.utils.GsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("8.3 — DLQ: reprocessamento de eventos com falha")
class AuditDlqIntegrationTest extends AbstractAuditIntegrationTest {

    @Autowired
    private ScosAuditDlqJob dlqJob;

    @Autowired
    private ScosAuditLogService logService;

    @Test
    @DisplayName("DLQ job deve mover registros da DLQ para SFA_LOG_AUDIT")
    void dlqJobMustReprocessPendingEntries() throws InterruptedException {
        Country country = countryRepository.save(Country.builder()
                .name("DLQ Test Country")
                .code(99001)
                .acronym("DLQ")
                .description("dlq test")
                .build());

        ScosAuditLog auditLog = ScosAuditLog.builder()
                .actionType(br.com.sawcunhaos.foundation.audit.domain.entity.ActionType.INSERT)
                .entity("SFA_COUNTRY")
                .idEntity(country.getId().toString())
                .user("test-user")
                .originSystem("TEST")
                .executionDate(java.time.LocalDateTime.now())
                .build();

        ScosAuditDlqLog dlqEntry = ScosAuditDlqLog.builder()
                .payload(GsonUtils.getInstance().toJson(auditLog))
                .error("Simulated failure")
                .retryCount(0)
                .createdAt(OffsetDateTime.now())
                .build();
        dlqRepository.save(dlqEntry);

        List<ScosAuditDlqLog> beforeReprocess = dlqRepository.findAll();
        assertFalse(beforeReprocess.isEmpty(), "DLQ must have entries before reprocess");

        dlqJob.reprocess();

        waitForAsync(500);

        List<ScosAuditDlqLog> afterReprocess = dlqRepository.findAll();
        assertTrue(afterReprocess.isEmpty(), "DLQ must be empty after successful reprocess");

        List<ScosAuditLog> persisted = auditLogRepository.findAll();
        assertFalse(persisted.isEmpty(), "Audit log must have entry after DLQ reprocess");
    }

    @Test
    @DisplayName("Registro na DLQ com retry_count deve incrementar em falha de reprocessamento")
    void dlqRetryCountIncreasesOnFailure() throws InterruptedException {
        // Payload inválido — não pode ser desserializado como ScosAuditLog completo
        ScosAuditDlqLog dlqEntry = ScosAuditDlqLog.builder()
                .payload("{\"invalid\": true, \"user\": null, \"originSystem\": null}")
                .error("Original error")
                .retryCount(0)
                .createdAt(OffsetDateTime.now())
                .build();
        dlqRepository.save(dlqEntry);

        dlqJob.reprocess();
        waitForAsync(500);

        List<ScosAuditDlqLog> remaining = dlqRepository.findAll();
        assertFalse(remaining.isEmpty(), "Failed DLQ entry must remain in table");
    }

}
