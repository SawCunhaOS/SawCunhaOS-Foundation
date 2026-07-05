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

import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Contrato para consulta paginada da trilha de auditoria.
 *
 * <p>Todas as queries operam no datasource dedicado de auditoria
 * ({@code spring.datasource.audit}) e não interferem no datasource de negócio.
 *
 * @since 1.2.0
 */
public interface ScosAuditQueryService {

    /**
     * Retorna a trilha de auditoria de um registro específico.
     *
     * @param entity   nome da entidade/tabela auditada (ex: {@code "SFA_PEDIDO"})
     * @param idEntity identificador do registro auditado
     * @param pageable paginação e ordenação; recomenda-se ordenar por {@code executionDate} descendente
     * @return página de logs do registro
     */
    Page<ScosAuditLog> findByEntity(String entity, String idEntity, Pageable pageable);

    /**
     * Retorna todos os eventos de auditoria gerados por um usuário.
     *
     * @param user     identificador do usuário (valor gravado em {@link ScosAuditLog#getUser()})
     * @param pageable paginação e ordenação
     * @return página de logs do usuário
     */
    Page<ScosAuditLog> findByUser(String user, Pageable pageable);

    /**
     * Retorna eventos de auditoria dentro de um intervalo de tempo.
     *
     * @param start    início do intervalo (inclusive)
     * @param end      fim do intervalo (inclusive)
     * @param pageable paginação e ordenação
     * @return página de logs no período
     */
    Page<ScosAuditLog> findByPeriod(LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * Localiza o evento de auditoria associado a um identificador de requisição.
     *
     * @param xRequestId valor do header {@code X-Request-ID} da requisição de origem
     * @return evento correspondente, ou {@link java.util.Optional#empty()} se não encontrado
     */
    Optional<ScosAuditLog> findByXRequestId(String xRequestId);

}
