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

import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditLogProperties;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import br.com.sawcunhaos.foundation.audit.domain.repository.ScosAuditLogRepository;
import br.com.sawcunhaos.foundation.audit.specification.ScosAuditQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@ConditionalOnProperty(prefix = "scos.audit", name = "enabled", havingValue = "true")
@Service
@RequiredArgsConstructor
public class ScosAuditQueryServiceBean implements ScosAuditQueryService {

    private final ScosAuditLogRepository repository;
    private final ScosAuditLogProperties logProperties;

    @Override
    @Transactional(value = "ScosAuditLogTransactionManager", readOnly = true)
    public Page<ScosAuditLog> findByEntity(String entity, String idEntity, Pageable pageable) {
        return repository.findByEntityAndIdEntityAndOriginSystem(
                entity.toUpperCase(), idEntity, logProperties.getSystem(), pageable);
    }

    @Override
    @Transactional(value = "ScosAuditLogTransactionManager", readOnly = true)
    public Page<ScosAuditLog> findByUser(String user, Pageable pageable) {
        return repository.findByUserAndOriginSystem(user, logProperties.getSystem(), pageable);
    }

    @Override
    @Transactional(value = "ScosAuditLogTransactionManager", readOnly = true)
    public Page<ScosAuditLog> findByPeriod(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start date must be before or equal to end date");
        }
        return repository.findByExecutionDateBetweenAndOriginSystem(
                start, end, logProperties.getSystem(), pageable);
    }

    @Override
    @Transactional(value = "ScosAuditLogTransactionManager", readOnly = true)
    public Optional<ScosAuditLog> findByXRequestId(String xRequestId) {
        return repository.findByXRequestIdAndOriginSystem(xRequestId, logProperties.getSystem());
    }

}
