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

import br.com.sawcunhaos.foundation.audit.domain.repository.CountryRepository;
import br.com.sawcunhaos.foundation.audit.domain.repository.ScosAuditDlqRepository;
import br.com.sawcunhaos.foundation.audit.domain.repository.ScosAuditLogRepository;
import br.com.sawcunhaos.foundation.audit.service.ScosAuditQueue;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = AuditTestApplication.class)
@Testcontainers
@ActiveProfiles("postgres")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractAuditIntegrationTest {

    @Container
    protected static PostgreSQLContainer container = new PostgreSQLContainer(
            DockerImageName.parse("postgis/postgis:18-3.6-alpine").asCompatibleSubstituteFor("postgres")
    );

    @DynamicPropertySource
    static void setupProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.audit.url", container::getJdbcUrl);
        registry.add("spring.datasource.audit.username", container::getUsername);
        registry.add("spring.datasource.audit.password", container::getPassword);
        registry.add("spring.datasource.audit.driver-class-name", container::getDriverClassName);
    }

    @Autowired
    protected CountryRepository countryRepository;

    @Autowired
    protected ScosAuditLogRepository auditLogRepository;

    @Autowired
    protected ScosAuditDlqRepository dlqRepository;

    @Autowired
    protected ScosAuditQueue auditQueue;

    @BeforeEach
    void cleanUp() {
        try {
            // Deleting countries fires @Async DELETE audit events into the queue.
            countryRepository.deleteAll();
            // Wait for the queue to fully drain so no in-flight event leaks into the next test.
            awaitQueueEmpty(5000);
            // Now the audit table holds only those drained DELETE events — wipe it clean.
            auditLogRepository.deleteAll();
            dlqRepository.deleteAll();
        } catch (Exception e) {
            // Context may still be initializing on first run — safe to ignore here
        }
    }

    private void awaitQueueEmpty(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (auditQueue.isEmpty()) {
                Thread.sleep(200);
                if (auditQueue.isEmpty()) return;
            }
            Thread.sleep(50);
        }
    }

    protected void waitForAsync(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    /**
     * Polls until the audit log table holds at least {@code expected} rows, or the timeout elapses.
     * Robust against async batch latency under load instead of relying on a fixed sleep.
     */
    protected void awaitAuditCountAtLeast(long expected, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (auditLogRepository.count() >= expected) return;
            Thread.sleep(50);
        }
    }

}
