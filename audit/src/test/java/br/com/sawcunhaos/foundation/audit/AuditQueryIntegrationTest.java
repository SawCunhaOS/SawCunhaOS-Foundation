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
import br.com.sawcunhaos.foundation.audit.specification.ScosAuditQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("8.6 — Query: consultas paginadas da trilha")
class AuditQueryIntegrationTest extends AbstractAuditIntegrationTest {

    @Autowired
    private ScosAuditQueryService queryService;

    @Test
    @DisplayName("findByEntity retorna trilha paginada da entidade correta")
    void findByEntityReturnsPaginatedResults() throws InterruptedException {
        Country country = countryRepository.save(Country.builder()
                .name("Query Test")
                .code(70001)
                .acronym("QT1")
                .description("query test")
                .build());
        String id = country.getId().toString();

        country.setCode(70002);
        countryRepository.save(country);

        waitForAsync(2000);

        Page<ScosAuditLog> page = queryService.findByEntity("SFA_COUNTRY", id,
                PageRequest.of(0, 10, Sort.by("executionDate")));

        assertEquals(2, page.getTotalElements(), "Must return INSERT + UPDATE for this entity");
        assertTrue(page.getContent().stream().allMatch(l -> l.getIdEntity().equals(id)));
    }

    @Test
    @DisplayName("Consulta isola por scos.audit.system: registros de outro sistema não aparecem")
    void queryIsolatesByOriginSystem() {
        String sharedId = UUID.randomUUID().toString();

        // Record from THIS system (SCOS_AUDIT) — must appear
        auditLogRepository.save(ScosAuditLog.builder()
                .actionType(ActionType.INSERT)
                .entity("SFA_COUNTRY")
                .idEntity(sharedId)
                .user("alice")
                .originSystem("SCOS_AUDIT")
                .executionDate(LocalDateTime.now())
                .build());

        // Record from ANOTHER system — must be filtered out
        auditLogRepository.save(ScosAuditLog.builder()
                .actionType(ActionType.INSERT)
                .entity("SFA_COUNTRY")
                .idEntity(sharedId)
                .user("alice")
                .originSystem("OTHER_SYSTEM")
                .executionDate(LocalDateTime.now())
                .build());

        Page<ScosAuditLog> byEntity = queryService.findByEntity("SFA_COUNTRY", sharedId, PageRequest.of(0, 10));
        assertEquals(1, byEntity.getTotalElements(), "Only this system's record must be visible");
        assertTrue(byEntity.getContent().stream().allMatch(l -> "SCOS_AUDIT".equals(l.getOriginSystem())));

        Page<ScosAuditLog> byUser = queryService.findByUser("alice", PageRequest.of(0, 10));
        assertTrue(byUser.getContent().stream().allMatch(l -> "SCOS_AUDIT".equals(l.getOriginSystem())),
                "findByUser must not return other systems' records");
    }

    @Test
    @DisplayName("findByEntity com ID inexistente retorna página vazia")
    void findByEntityWithUnknownIdReturnsEmpty() {
        Page<ScosAuditLog> page = queryService.findByEntity("SFA_COUNTRY", UUID.randomUUID().toString(),
                PageRequest.of(0, 10));
        assertEquals(0, page.getTotalElements());
    }

    @Test
    @DisplayName("findByUser retorna todos os eventos do ator")
    void findByUserReturnsAllActorEvents() throws InterruptedException {
        countryRepository.save(Country.builder()
                .name("User Query")
                .code(70100)
                .acronym("UQ1")
                .build());

        waitForAsync(2000);

        Page<ScosAuditLog> page = queryService.findByUser("Test", PageRequest.of(0, 20));
        assertFalse(page.isEmpty(), "Must return events for user 'Test'");
        assertTrue(page.getContent().stream().allMatch(l -> "Test".equals(l.getUser())));
    }

    @Test
    @DisplayName("findByPeriod retorna apenas registros dentro do intervalo")
    void findByPeriodFiltersCorrectly() throws InterruptedException {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        countryRepository.save(Country.builder()
                .name("Period Query")
                .code(70200)
                .acronym("PQ1")
                .build());

        waitForAsync(2000);

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        Page<ScosAuditLog> page = queryService.findByPeriod(before, after, PageRequest.of(0, 10));
        assertFalse(page.isEmpty(), "Must return records within period");
    }

    @Test
    @DisplayName("findByPeriod com ini > fim lança IllegalArgumentException")
    void findByPeriodWithInvalidRangeThrows() {
        LocalDateTime now = LocalDateTime.now();
        assertThrows(IllegalArgumentException.class,
                () -> queryService.findByPeriod(now.plusHours(1), now, PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("findByXRequestId retorna registro com xRequestId específico")
    void findByXRequestIdReturnsCorrectRecord() throws InterruptedException {
        ScosAuditLog log = ScosAuditLog.builder()
                .actionType(ActionType.INSERT)
                .entity("SFA_COUNTRY")
                .idEntity(UUID.randomUUID().toString())
                .user("test")
                .originSystem("SCOS_AUDIT")  // must match scos.audit.system from test profile
                .executionDate(LocalDateTime.now())
                .xRequestId("req-unique-xyz-123")
                .build();
        auditLogRepository.save(log);

        Optional<ScosAuditLog> found = queryService.findByXRequestId("req-unique-xyz-123");
        assertTrue(found.isPresent());
        assertEquals("req-unique-xyz-123", found.get().getXRequestId());
    }

}
