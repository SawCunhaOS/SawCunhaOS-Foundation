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

package br.com.sawcunhaos.foundation.audit.service;

import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditRetentionProperties;
import br.com.sawcunhaos.foundation.audit.domain.entity.ActionType;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import br.com.sawcunhaos.foundation.audit.domain.repository.ScosAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ConditionalOnProperty(prefix = "scos.audit.retention", name = "enabled", havingValue = "true")
@ConditionalOnBean(ScosAuditLogService.class)
@Component
@RequiredArgsConstructor
@Slf4j
public class ScosAuditRetentionJob {

    private final ScosAuditLogRepository repository;
    private final ScosAuditHashService hashService;
    private final ScosAuditRetentionProperties retentionProperties;
    private final ScosAuditLogService logService;

    @Scheduled(cron = "${scos.audit.retention.cron:0 0 2 * * *}")
    @Transactional("ScosAuditLogTransactionManager")
    public void purgeExpiredRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionProperties.getTtlDays());
        List<ScosAuditLog> expired = repository.findByExecutionDateBetween(
                LocalDateTime.of(2000, 1, 1, 0, 0), cutoff,
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        if (expired.isEmpty()) return;

        log.info("Purging {} expired audit records (cutoff={})", expired.size(), cutoff);

        for (ScosAuditLog record : expired) {
            if (record.getHashChain() != null) {
                ScosAuditLog tombstone = buildTombstone(record);
                logService.saveBatch(List.of(tombstone));
            }
            repository.delete(record);
        }

        log.info("Purge complete: {} records removed, tombstones inserted for records with hash chain", expired.size());
    }

    private ScosAuditLog buildTombstone(ScosAuditLog expiredRecord) {
        String tombstoneHash = hashService.computeHash(
                ScosAuditLog.builder()
                        .entity(expiredRecord.getEntity())
                        .idEntity(expiredRecord.getIdEntity())
                        .actionType(ActionType.TOMBSTONE)
                        .executionDate(LocalDateTime.now())
                        .build(),
                expiredRecord.getHashChain()
        );
        return ScosAuditLog.builder()
                .actionType(ActionType.TOMBSTONE)
                .entity(expiredRecord.getEntity())
                .idEntity(expiredRecord.getIdEntity())
                .user("SYSTEM")
                .originSystem(expiredRecord.getOriginSystem())
                .executionDate(LocalDateTime.now())
                .hashChain(tombstoneHash)
                .build();
    }

}
