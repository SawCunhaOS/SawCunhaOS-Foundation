
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

package br.com.sawcunhaos.foundation.audit.specification;

import org.hibernate.event.spi.AbstractEvent;

/**
 * Contrato para registro de eventos de auditoria na trilha {@code SFA_LOG_AUDIT}.
 *
 * <p>Implementações recebem eventos Hibernate ({@link AbstractEvent}) ou chamadas
 * diretas para operações de leitura, serializam o estado da entidade e enfileiram
 * o log para persistência assíncrona.
 *
 * @since 1.2.0
 */
public interface ScosAuditService {

    /**
     * Registra um evento de escrita (INSERT, UPDATE ou DELETE) capturado pelo listener Hibernate.
     *
     * @param listener   evento Hibernate pós-operação ({@code PostInsertEvent},
     *                   {@code PostUpdateEvent} ou {@code PostDeleteEvent})
     * @param user       identificador do usuário autenticado no momento do evento
     * @param ipAddress  endereço IP da requisição de origem
     * @param xRequestId identificador único da requisição ({@code X-Request-ID})
     */
    void saveAuditLog(AbstractEvent listener, String user, String ipAddress, String xRequestId);

    /**
     * Registra explicitamente uma operação de leitura de dado sensível.
     *
     * <p>Utilize quando a leitura não é capturada automaticamente pelo aspecto
     * {@code ScosAuditReadAspect} — por exemplo, em queries JPQL ou operações bulk.
     *
     * @param entity   nome da entidade/tabela auditada (ex: {@code "SFA_PEDIDO"})
     * @param idEntity identificador do registro lido
     */
    void recordRead(final String entity, final String idEntity, final String user, final String ipAddress, final String xRequestId);

}
