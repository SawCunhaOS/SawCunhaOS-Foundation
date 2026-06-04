
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

/**
 * Tipos de operação registrados na trilha de auditoria.
 *
 * @since 1.2.0
 */
public enum ActionType {

    /**
     * Leitura explícita de dado sensível, registrada via {@code ScosAuditReadAspect}
     * (em métodos anotados com {@code @Auditable(action = AuditAction.READ)}) ou
     * por chamada direta a {@link ScosAuditService#recordRead}.
     */
    SELECT,

    /**
     * Atualização de registro existente, capturada pelo listener Hibernate
     * ({@code PostUpdateEvent}) em entidades anotadas com {@code @Auditable}.
     */
    UPDATE,

    /**
     * Inserção de novo registro, capturada pelo listener Hibernate
     * ({@code PostInsertEvent}) em entidades anotadas com {@code @Auditable}.
     */
    INSERT,

    /**
     * Remoção de registro, capturada pelo listener Hibernate
     * ({@code PostDeleteEvent}) em entidades anotadas com {@code @Auditable}.
     */
    DELETE,

    /**
     * Marcador de exclusão inserido pelo job de retenção ({@code ScosAuditRetentionJob})
     * ao deletar registros expirados. Preserva a continuidade da hash-chain quando
     * {@code scos.audit.immutability.hash-chain=true}.
     */
    TOMBSTONE;
}
