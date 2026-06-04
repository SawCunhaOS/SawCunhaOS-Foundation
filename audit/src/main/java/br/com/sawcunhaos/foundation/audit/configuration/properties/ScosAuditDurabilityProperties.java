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

package br.com.sawcunhaos.foundation.audit.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties de durabilidade do pipeline de auditoria ({@code scos.audit.durability.*}).
 *
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "scos.audit.durability")
@Data
public class ScosAuditDurabilityProperties {

    /** Número máximo de tentativas de persistência de um lote antes de rotear os eventos para a DLQ. O backoff entre tentativas é exponencial (base 2 × 100 ms). Padrão: {@code 3}. */
    private int retryMax = 3;

    /** Habilita o roteamento para a DLQ ({@code SFA_AUDIT_DLQ}) quando todas as tentativas falham. {@code false} = eventos são descartados silenciosamente (não recomendado). Padrão: {@code true}. */
    private boolean dlqEnabled = true;

}
