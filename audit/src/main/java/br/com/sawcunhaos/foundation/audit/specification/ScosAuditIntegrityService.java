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

/**
 * Contrato para verificação de integridade da trilha de auditoria via hash-chain.
 *
 * <p>Disponível apenas quando {@code scos.audit.immutability.hash-chain=true}.
 * A verificação recalcula o hash SHA-256 de cada evento na sequência e compara
 * com o valor persitido — qualquer divergência indica adulteração ou remoção de registro.
 *
 * @since 1.2.0
 */
public interface ScosAuditIntegrityService {

    /**
     * Verifica a integridade da cadeia de hash para um registro específico.
     *
     * @param entity   nome da entidade/tabela auditada (ex: {@code "SFA_PEDIDO"})
     * @param idEntity identificador do registro auditado
     * @return {@code true} se a cadeia está íntegra; {@code false} se adulteração ou
     *         remoção de registro for detectada
     */
    boolean verifyChain(String entity, String idEntity);

}
