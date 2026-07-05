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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScosAuditLogRepository extends JpaRepository<ScosAuditLog, UUID> {

    Optional<ScosAuditLog> findByActionType(ActionType actionType);

    // --- queries with originSystem filter (multi-tenancy by system) ---

    Page<ScosAuditLog> findByEntityAndIdEntityAndOriginSystem(String entity, String idEntity, String originSystem, Pageable pageable);

    Page<ScosAuditLog> findByUserAndOriginSystem(String user, String originSystem, Pageable pageable);

    Page<ScosAuditLog> findByExecutionDateBetweenAndOriginSystem(LocalDateTime start, LocalDateTime end, String originSystem, Pageable pageable);

    @Query("SELECT s FROM ScosAuditLog s WHERE s.xRequestId = :xRequestId AND s.originSystem = :originSystem")
    Optional<ScosAuditLog> findByXRequestIdAndOriginSystem(@Param("xRequestId") String xRequestId, @Param("originSystem") String originSystem);

    // --- internal / integrity queries (no originSystem filter) ---

    Page<ScosAuditLog> findByExecutionDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT s FROM ScosAuditLog s WHERE s.xRequestId = :xRequestId")
    Optional<ScosAuditLog> findByXRequestId(@Param("xRequestId") String xRequestId);

    @Query("SELECT s FROM ScosAuditLog s WHERE s.entity = :entity AND s.idEntity = :idEntity ORDER BY s.executionDate DESC LIMIT 1")
    Optional<ScosAuditLog> findLastByEntityAndIdEntity(@Param("entity") String entity, @Param("idEntity") String idEntity);

    List<ScosAuditLog> findAllByEntityAndIdEntityOrderByEventOrderAsc(String entity, String idEntity);

}
