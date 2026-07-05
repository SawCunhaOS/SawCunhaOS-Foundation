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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("8.9 — Métricas: gauge, distribution summary e counter")
class AuditMetricsIntegrationTest extends AbstractAuditIntegrationTest {

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("audit.batch.size registra observações após burst")
    void batchSizeSummaryRecordsObservations() throws InterruptedException {
        if (meterRegistry == null) {
            return;
        }

        List<Country> countries = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            countries.add(Country.builder()
                    .name("Metrics-" + i)
                    .code(95000 + i)
                    .acronym("MT" + (i % 99))
                    .build());
        }
        countryRepository.saveAll(countries);

        waitForAsync(3000);

        DistributionSummary summary = meterRegistry.find("audit.batch.size").summary();
        assertNotNull(summary, "audit.batch.size metric must be registered");
        assertTrue(summary.count() > 0, "audit.batch.size must record at least one observation");
    }

    @Test
    @DisplayName("audit.queue.depth gauge está registrado")
    void queueDepthGaugeIsRegistered() {
        if (meterRegistry == null) {
            return;
        }

        Gauge gauge = meterRegistry.find("audit.queue.depth").gauge();
        assertNotNull(gauge, "audit.queue.depth gauge must be registered");
    }

    @Test
    @DisplayName("audit.events.dlq counter está registrado")
    void dlqCounterIsRegistered() {
        if (meterRegistry == null) {
            return;
        }

        Counter counter = meterRegistry.find("audit.events.dlq").counter();
        assertNotNull(counter, "audit.events.dlq counter must be registered");
    }

}
