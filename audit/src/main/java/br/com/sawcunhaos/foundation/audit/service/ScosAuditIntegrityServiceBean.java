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

import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditImmutabilityProperties;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import br.com.sawcunhaos.foundation.audit.domain.repository.ScosAuditLogRepository;
import br.com.sawcunhaos.foundation.audit.specification.ScosAuditIntegrityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ConditionalOnProperty(prefix = "scos.audit", name = "enabled", havingValue = "true")
@Service
@RequiredArgsConstructor
@Slf4j
public class ScosAuditIntegrityServiceBean implements ScosAuditIntegrityService {

    private final ScosAuditLogRepository repository;
    private final ScosAuditHashService hashService;
    private final ScosAuditImmutabilityProperties immutabilityProperties;

    @Override
    @Transactional(value = "ScosAuditLogTransactionManager", readOnly = true)
    public boolean verifyChain(String entity, String idEntity) {
        if (!immutabilityProperties.isHashChain()) {
            throw new UnsupportedOperationException("hash-chain not enabled");
        }

        List<ScosAuditLog> chain = repository.findAllByEntityAndIdEntityOrderByEventOrderAsc(
                entity.toUpperCase(), idEntity);

        if (chain.isEmpty()) return true;

        boolean allNull = chain.stream().allMatch(l -> l.getHashChain() == null);
        if (allNull) {
            log.warn("No hash chain found for entity={} idEntity={} — records pre-date migration, chain not verifiable", entity, idEntity);
            return true;
        }

        String previousHash = ScosAuditHashService.genesisHash();
        for (ScosAuditLog record : chain) {
            if (record.getHashChain() == null) {
                previousHash = ScosAuditHashService.genesisHash();
                continue;
            }
            String expectedHash = hashService.computeHash(record, previousHash);
            if (!expectedHash.equals(record.getHashChain())) {
                log.error("Hash chain broken at record id={} entity={} idEntity={} (tampering or missing record detected)",
                        record.getId(), entity, idEntity);
                return false;
            }
            previousHash = record.getHashChain();
        }
        return true;
    }

}
