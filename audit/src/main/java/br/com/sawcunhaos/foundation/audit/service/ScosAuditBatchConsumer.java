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

import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditDurabilityProperties;
import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditImmutabilityProperties;
import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditPerformanceProperties;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditDlqLog;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import br.com.sawcunhaos.foundation.audit.domain.repository.ScosAuditDlqRepository;
import br.com.sawcunhaos.foundation.utils.utils.GsonUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@ConditionalOnProperty(prefix = "scos.audit", name = "enabled", havingValue = "true")
@Component
@Slf4j
public class ScosAuditBatchConsumer {

    private final ScosAuditQueue queue;
    private final ScosAuditLogService logService;
    private final ScosAuditDlqRepository dlqRepository;
    private final ScosAuditHashService hashService;
    private final ScosAuditPerformanceProperties performanceProps;
    private final ScosAuditDurabilityProperties durabilityProps;
    private final ScosAuditImmutabilityProperties immutabilityProps;

    private static final AtomicLong EVENT_ORDER_SEQ = new AtomicLong(0);

    /** Bounds work per scheduler tick so a runaway queue cannot starve the scheduler thread. */
    private static final int MAX_BATCHES_PER_TICK = 1000;

    private Counter dlqCounter;
    private DistributionSummary batchSizeSummary;

    public ScosAuditBatchConsumer(
            ScosAuditQueue queue,
            ScosAuditLogService logService,
            ScosAuditDlqRepository dlqRepository,
            ScosAuditHashService hashService,
            ScosAuditPerformanceProperties performanceProps,
            ScosAuditDurabilityProperties durabilityProps,
            ScosAuditImmutabilityProperties immutabilityProps,
            ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.queue = queue;
        this.logService = logService;
        this.dlqRepository = dlqRepository;
        this.hashService = hashService;
        this.performanceProps = performanceProps;
        this.durabilityProps = durabilityProps;
        this.immutabilityProps = immutabilityProps;

        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry != null) {
            Gauge.builder("audit.queue.depth", queue, ScosAuditQueue::size)
                    .description("Current number of audit events waiting in the in-memory queue")
                    .register(registry);
            this.batchSizeSummary = DistributionSummary.builder("audit.batch.size")
                    .description("Size of each audit batch persisted")
                    .register(registry);
            this.dlqCounter = Counter.builder("audit.events.dlq")
                    .description("Number of audit events sent to DLQ")
                    .register(registry);
        }
    }

    /**
     * Drains the in-memory queue in batches on a fixed schedule. Using Spring's {@code @Scheduled}
     * (instead of a manually managed thread) makes the consumer resilient to context lifecycle:
     * the scheduler is started/stopped by Spring and a failing tick never kills future ticks.
     * Each tick drains until the queue is empty (bounded by {@link #MAX_BATCHES_PER_TICK}), so a
     * burst is absorbed within a single flush interval rather than one batch per tick.
     */
    @Scheduled(fixedDelayString = "${scos.audit.performance.flush-interval-ms:500}")
    public void drainScheduled() {
        int iterations = 0;
        while (!queue.isEmpty() && iterations < MAX_BATCHES_PER_TICK) {
            try {
                drainAndPersist();
            } catch (Throwable t) {
                // Never let a single batch failure abort the tick or kill the schedule.
                log.error("Unexpected error draining audit batch — continuing", t);
            }
            iterations++;
        }
    }

    private void drainAndPersist() {
        List<ScosAuditLog> batch = new ArrayList<>(performanceProps.getBatchSize());
        queue.drainTo(batch, performanceProps.getBatchSize());

        if (batch.isEmpty()) return;

        batch.forEach(e -> {
            // Monotonic event order: stable sort for chain verification even when multiple
            // events share the same executionDate (rapid successive operations).
            e.setEventOrder(EVENT_ORDER_SEQ.incrementAndGet());
            // Truncate executionDate to microseconds BEFORE persisting. PostgreSQL TIMESTAMP
            // rounds nanoseconds to microseconds while the hash uses Java truncation — pre-truncating
            // makes the stored value and the hashed value identical, keeping the chain verifiable.
            if (e.getExecutionDate() != null) {
                e.setExecutionDate(e.getExecutionDate().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
            }
        });

        if (immutabilityProps.isHashChain()) {
            applyHashChain(batch);
        }

        persistWithRetry(batch);
    }

    private void applyHashChain(List<ScosAuditLog> batch) {
        // Track the last computed hash within the batch per (entity, idEntity).
        // Querying DB for each event would miss events in the same batch that
        // haven't been persisted yet, breaking the chain for multi-event batches.
        java.util.Map<String, String> batchHashTracker = new java.util.HashMap<>();

        for (ScosAuditLog auditEntry : batch) {
            String key = auditEntry.getEntity() + ":" + auditEntry.getIdEntity();
            String previousHash = batchHashTracker.get(key);
            if (previousHash == null) {
                previousHash = hashService.findLastHashFromDb(auditEntry.getEntity(), auditEntry.getIdEntity());
            }
            String hash = hashService.computeHash(auditEntry, previousHash);
            auditEntry.setHashChain(hash);
            batchHashTracker.put(key, hash);
        }
    }

    private void persistWithRetry(List<ScosAuditLog> batch) {
        int attempts = 0;
        int maxAttempts = durabilityProps.getRetryMax();
        while (attempts < maxAttempts) {
            try {
                logService.saveBatch(batch);
                if (batchSizeSummary != null) {
                    batchSizeSummary.record(batch.size());
                }
                return;
            } catch (Exception e) {
                attempts++;
                if (attempts >= maxAttempts) {
                    log.error("Audit batch failed after {} attempts, routing {} events to DLQ", maxAttempts, batch.size(), e);
                    routeToDlq(batch, e.getMessage());
                    return;
                }
                long backoff = (long) Math.pow(2, attempts) * 100L;
                log.warn("Audit batch persist failed (attempt {}/{}), retrying in {}ms", attempts, maxAttempts, backoff, e);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    routeToDlq(batch, "Consumer interrupted during retry: " + e.getMessage());
                    return;
                }
            }
        }
    }

    private void routeToDlq(List<ScosAuditLog> batch, String error) {
        if (!durabilityProps.isDlqEnabled()) {
            log.error("DLQ disabled — {} audit events permanently lost", batch.size());
            return;
        }
        for (ScosAuditLog auditEntry : batch) {
            try {
                ScosAuditDlqLog dlq = ScosAuditDlqLog.builder()
                        .payload(GsonUtils.getInstance().toJson(auditEntry))
                        .error(error)
                        .retryCount(0)
                        .createdAt(OffsetDateTime.now())
                        .build();
                dlqRepository.save(dlq);
                if (dlqCounter != null) {
                    dlqCounter.increment();
                }
            } catch (Exception e) {
                log.error("Failed to save event to DLQ — event permanently lost", e);
            }
        }
    }

    public void offerOrDlq(ScosAuditLog auditLog) {
        if (!queue.offer(auditLog)) {
            log.warn("Audit queue full (capacity={}), routing event to DLQ", performanceProps.getQueueCapacity());
            routeToDlq(List.of(auditLog), "Queue capacity exceeded");
        }
    }

}
