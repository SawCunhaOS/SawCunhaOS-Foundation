
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

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro imutável de um evento de auditoria persistido em {@code SFA_LOG_AUDIT}.
 *
 * <p>Cada instância representa uma operação de leitura ou escrita capturada pelo
 * módulo audit. O estado anterior e posterior da entidade é serializado em JSONB
 * com chaves ordenadas alfabeticamente para garantir consistência na hash-chain.
 *
 * @since 1.2.0
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
@Table(name = "SFA_LOG_AUDIT")
@Entity
// Explicit null policy for the DLQ payload (Jackson serializes the whole entity there): always
// include null fields, matching the old GsonUtils instance (serializeNulls()) — see audit/README.md.
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ScosAuditLog {

    /** Identificador único do registro de auditoria (UUID gerado automaticamente). */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_LOG")
    private UUID id;

    /** Nome do sistema de origem, conforme {@code scos.audit.system}. */
    @Column(name = "ORIGIN_SYSTEM")
    private String originSystem;

    /** Tipo da operação auditada. */
    @Enumerated(EnumType.STRING)
    @Column(name = "ACTION_TYPE")
    private ActionType actionType;

    /** Identificador do registro auditado na entidade de origem. */
    @Column(name = "ID_ENTITY")
    private String idEntity;

    /** Nome da tabela/entidade auditada em maiúsculas (ex: {@code "SFA_PEDIDO"}). */
    @Column(name = "ENTITY")
    private String entity;

    /** Estado anterior da entidade serializado em JSONB. {@code null} em operações INSERT. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ENTITY_OLD", columnDefinition = "jsonb")
    private String entityOld;

    /** Estado posterior da entidade serializado em JSONB. {@code null} em operações DELETE. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ENTITY_NEW", columnDefinition = "jsonb")
    private String entityNew;

    /** Identificador do usuário autenticado no momento do evento; {@code "SYSTEM"} quando não há contexto de autenticação. */
    @Column(name = "LOGGED_USER")
    private String user;

    /** Data e hora de execução do evento, truncada a microssegundos para compatibilidade com {@code TIMESTAMP} do PostgreSQL. */
    @Column(name = "EXECUTION_DATE")
    private LocalDateTime executionDate;

    /** Endereço IP da requisição de origem, obtido do MDC ({@code IS_IP}). */
    @Column(name = "IP_ADDRESS")
    private String ipAddress;

    /** Identificador único da requisição HTTP de origem ({@code X-Request-ID}), obtido do MDC. */
    @Column(name = "X_REQUEST_ID")
    private String xRequestId;

    /** Hash SHA-256 encadeado com o evento anterior do mesmo registro. Preenchido apenas quando {@code scos.audit.immutability.hash-chain=true}. */
    @Column(name = "HASH_CHAIN", length = 64)
    private String hashChain;

    /** Número de ordem monotônico do evento, usado como critério de ordenação estável na verificação da hash-chain. */
    @Column(name = "EVENT_ORDER")
    private Long eventOrder;

}
