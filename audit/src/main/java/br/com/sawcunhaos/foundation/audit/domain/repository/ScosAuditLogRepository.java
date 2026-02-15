
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

package br.com.sawcunhaos.foundation.audit.domain.repository;

import br.com.sawcunhaos.foundation.audit.domain.entity.ActionType;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScosAuditLogRepository extends JpaRepository<ScosAuditLog, UUID> {

    Optional<ScosAuditLog> findByActionType(ActionType actionType);

}
