
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

import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import br.com.sawcunhaos.foundation.audit.domain.repository.ScosAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@ConditionalOnProperty(prefix="scos.audit", name = "enabled", havingValue = "true")
@Service
@RequiredArgsConstructor
public class ScosAuditLogService {

    private final ScosAuditLogRepository scosAuditLogRepository;

    @Transactional("ScosAuditLogTransactionManager")
    public void saveLog(final ScosAuditLog scosAuditLog) {
        scosAuditLogRepository.saveAndFlush(scosAuditLog);
    }


}
