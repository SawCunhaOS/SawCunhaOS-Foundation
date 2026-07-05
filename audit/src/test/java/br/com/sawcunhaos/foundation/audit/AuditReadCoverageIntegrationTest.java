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
import br.com.sawcunhaos.foundation.audit.service.CountryReadService;
import br.com.sawcunhaos.foundation.audit.specification.ScosAuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("8.7 — Read Coverage: emissão de SELECT via AOP e recordRead manual")
class AuditReadCoverageIntegrationTest extends AbstractAuditIntegrationTest {

    @Autowired
    private CountryReadService countryReadService;

    @Autowired
    private ScosAuditService auditService;

    @Test
    @DisplayName("@Auditable(READ) deve emitir ActionType.SELECT na trilha")
    void auditableReadEmitsSelectEvent() throws InterruptedException {
        Country country = countryRepository.save(Country.builder()
                .name("Read Coverage")
                .code(80001)
                .acronym("RC1")
                .description("read coverage test")
                .build());

        waitForAsync(1000);
        auditLogRepository.deleteAll();

        countryReadService.findById(country.getId());

        waitForAsync(1500);

        List<ScosAuditLog> logs = auditLogRepository.findAll();
        assertFalse(logs.isEmpty(), "READ event must be emitted");
        assertTrue(logs.stream().anyMatch(l -> l.getActionType() == ActionType.SELECT),
                "ActionType.SELECT must be present");
    }

    @Test
    @DisplayName("Método sem @Auditable não emite evento SELECT")
    void methodWithoutAnnotationDoesNotEmitEvent() throws InterruptedException {
        Country country = countryRepository.save(Country.builder()
                .name("No Audit")
                .code(80002)
                .acronym("NA1")
                .build());

        waitForAsync(1000);
        long countBefore = auditLogRepository.count();

        countryReadService.findByIdWithoutAudit(country.getId());

        waitForAsync(1000);
        long countAfter = auditLogRepository.count();

        assertEquals(countBefore, countAfter, "No new event must be emitted for unannotated method");
    }

}
