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

import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditDlqLog;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import br.com.sawcunhaos.foundation.audit.domain.repository.ScosAuditDlqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

/**
 * Job que reprocessa a DLQ ({@code SFA_AUDIT_DLQ}) em intervalo fixo
 * ({@code scos.audit.durability.dlq-reprocess-interval-ms}, padrão 60 s).
 *
 * <p>Lê até 50 entradas por ciclo; cada uma é regravada em {@code SFA_LOG_AUDIT} e removida da DLQ, ou
 * tem {@code retryCount} incrementado em caso de falha (sem limite máximo de tentativas).
 *
 * @since 1.2.0
 */
@ConditionalOnProperty(prefix = "scos.audit", name = "enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
@Slf4j
public class ScosAuditDlqJob {

    private static final int BATCH_LIMIT = 50;

    private final ScosAuditDlqRepository dlqRepository;
    private final ScosAuditLogService logService;
    private final ObjectMapper objectMapper;

    /**
     * Ciclo agendado: busca a primeira página (50) da DLQ e reprocessa cada entrada.
     */
    @Scheduled(fixedDelayString = "${scos.audit.durability.dlq-reprocess-interval-ms:60000}")
    public void reprocess() {
        List<ScosAuditDlqLog> pending = dlqRepository.findAllBy(PageRequest.of(0, BATCH_LIMIT));
        if (pending.isEmpty()) return;

        log.info("Reprocessing {} DLQ audit events", pending.size());

        for (ScosAuditDlqLog dlqEntry : pending) {
            reprocessEntry(dlqEntry.getId());
        }
    }

    /**
     * Reprocessa uma entrada: desserializa o payload, zera o {@code id} (novo UUID) e grava o evento;
     * em sucesso remove a entrada, em falha incrementa {@code retryCount} e guarda a mensagem de erro.
     * Chamado por {@link #reprocess()} dentro da mesma classe, portanto o {@code @Transactional} não
     * passa pelo proxy do Spring; cada operação do repositório roda na sua própria transação.
     *
     * @param dlqId identificador da entrada da DLQ
     */
    @Transactional("ScosAuditLogTransactionManager")
    public void reprocessEntry(UUID dlqId) {
        ScosAuditDlqLog dlqEntry = dlqRepository.findById(dlqId).orElse(null);
        if (dlqEntry == null) return;

        try {
            ScosAuditLog auditLog = objectMapper.readValue(dlqEntry.getPayload(), ScosAuditLog.class);
            auditLog.setId(null);
            logService.saveBatch(List.of(auditLog));
            dlqRepository.delete(dlqEntry);
        } catch (Exception e) {
            dlqEntry.setRetryCount(dlqEntry.getRetryCount() + 1);
            dlqEntry.setError(e.getMessage());
            dlqRepository.save(dlqEntry);
            log.warn("DLQ reprocess failed for entry {} (retry_count={})", dlqId, dlqEntry.getRetryCount(), e);
        }
    }

}
