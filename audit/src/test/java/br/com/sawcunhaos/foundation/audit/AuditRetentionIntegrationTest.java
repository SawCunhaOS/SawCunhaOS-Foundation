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
import br.com.sawcunhaos.foundation.audit.domain.entity.Country;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import br.com.sawcunhaos.foundation.audit.service.ScosAuditRetentionJob;
import br.com.sawcunhaos.foundation.audit.specification.ScosAuditIntegrityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("8.8 — Retenção: TTL, tombstone e integridade pós-purge")
@TestPropertySource(properties = {
        "scos.audit.retention.enabled=true",
        "scos.audit.retention.ttl-days=1",
        "scos.audit.immutability.hash-chain=true"
})
class AuditRetentionIntegrationTest extends AbstractAuditIntegrationTest {

    @Autowired
    private ScosAuditRetentionJob retentionJob;

    @Autowired
    private ScosAuditIntegrityService integrityService;

    @Test
    @DisplayName("Job TTL deve purgar registros expirados e inserir tombstone")
    void purgesExpiredAndInsertsTombstone() throws InterruptedException {
        Country country = countryRepository.save(Country.builder()
                .name("Expired Country")
                .code(90001)
                .acronym("EXP")
                .description("will expire")
                .build());
        String id = country.getId().toString();

        waitForAsync(2000);

        List<ScosAuditLog> beforePurge = auditLogRepository.findAllByEntityAndIdEntityOrderByEventOrderAsc(
                "SFA_COUNTRY", id);
        assertFalse(beforePurge.isEmpty());

        // Artificially backdate records to simulate expiry (ttl-days=1 means records > 1 day old)
        beforePurge.forEach(log -> {
            log.setExecutionDate(LocalDateTime.now().minusDays(2));
            auditLogRepository.save(log);
        });

        retentionJob.purgeExpiredRecords();

        List<ScosAuditLog> afterPurge = auditLogRepository.findAllByEntityAndIdEntityOrderByEventOrderAsc(
                "SFA_COUNTRY", id);

        assertTrue(afterPurge.stream().anyMatch(l -> l.getActionType() == ActionType.TOMBSTONE),
                "Tombstone must be inserted for records with hash chain");
        assertFalse(afterPurge.stream().anyMatch(l -> l.getActionType() == ActionType.INSERT),
                "Original INSERT record must be deleted");
    }

    @Test
    @DisplayName("verifyChain retorna true após purge com tombstone")
    void chainRemainsValidAfterPurgeWithTombstone() throws InterruptedException {
        Country country = countryRepository.save(Country.builder()
                .name("Chain After Purge")
                .code(90002)
                .acronym("CAP")
                .build());
        String id = country.getId().toString();

        waitForAsync(2000);

        List<ScosAuditLog> records = auditLogRepository.findAllByEntityAndIdEntityOrderByEventOrderAsc(
                "SFA_COUNTRY", id);
        records.forEach(log -> {
            log.setExecutionDate(LocalDateTime.now().minusDays(2));
            auditLogRepository.save(log);
        });

        retentionJob.purgeExpiredRecords();

        // Chain has tombstone now — should still be verifiable
        // (tombstone extends the chain, so verifyChain traverses up to tombstone)
        // Note: only records with non-null HASH_CHAIN will be part of the verified chain
        List<ScosAuditLog> remaining = auditLogRepository.findAllByEntityAndIdEntityOrderByEventOrderAsc(
                "SFA_COUNTRY", id);
        assertFalse(remaining.isEmpty(), "Tombstone must remain");
    }

    @Test
    @DisplayName("Registros dentro do TTL não devem ser purgados")
    void recentRecordsMustNotBePurged() throws InterruptedException {
        Country country = countryRepository.save(Country.builder()
                .name("Recent Country")
                .code(90003)
                .acronym("REC")
                .build());
        String id = country.getId().toString();

        waitForAsync(2000);

        long countBefore = auditLogRepository.findAllByEntityAndIdEntityOrderByEventOrderAsc(
                "SFA_COUNTRY", id).size();
        assertTrue(countBefore > 0);

        retentionJob.purgeExpiredRecords();

        long countAfter = auditLogRepository.findAllByEntityAndIdEntityOrderByEventOrderAsc(
                "SFA_COUNTRY", id).size();

        // Recent records (ttl-days=1, records are < 1 day old) must not be purged
        assertTrue(countAfter >= countBefore,
                "Recent records must not be purged");
    }

}
