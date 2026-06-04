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

package br.com.sawcunhaos.foundation.audit.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Registro de um evento de auditoria que falhou na persistência e aguarda reprocessamento em {@code SFA_AUDIT_DLQ}.
 *
 * <p>Eventos são roteados para esta fila quando o número de tentativas de persistência
 * excede {@code scos.audit.durability.retry-max}. O job {@code ScosAuditDlqJob}
 * reprocessa periodicamente os registros desta tabela.
 *
 * @since 1.2.0
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
@Table(name = "SFA_AUDIT_DLQ")
@Entity
public class ScosAuditDlqLog {

    /** Identificador único do registro na DLQ (UUID gerado automaticamente). */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    /** Serialização JSON do {@link ScosAuditLog} original que falhou na persistência. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "PAYLOAD", columnDefinition = "jsonb", nullable = false)
    private String payload;

    /** Mensagem de erro da última tentativa de persistência falha. */
    @Column(name = "ERROR", columnDefinition = "TEXT")
    private String error;

    /** Número de tentativas de reprocessamento já realizadas pelo {@code ScosAuditDlqJob}. Padrão: {@code 0}. */
    @Column(name = "RETRY_COUNT", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    /** Versão do registro para controle de concorrência otimista ({@code @Version}). */
    @Version
    @Column(name = "VERSION")
    @Builder.Default
    private Long version = 0L;

    /** Data e hora em que o evento foi roteado para a DLQ. */
    @Column(name = "CREATED_AT", nullable = false)
    private OffsetDateTime createdAt;

}
