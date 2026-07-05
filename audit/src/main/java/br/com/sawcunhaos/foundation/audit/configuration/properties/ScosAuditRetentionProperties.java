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

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties de retenção automática da trilha de auditoria ({@code scos.audit.retention.*}).
 *
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "scos.audit.retention")
@Data
public class ScosAuditRetentionProperties {

    /** Habilita o job de retenção automática. Quando {@code true}, {@link #ttlDays} é obrigatório. Padrão: {@code false}. */
    private boolean enabled = false;

    /** Tempo de vida dos registros de auditoria em dias. Registros mais antigos são deletados pelo job de retenção; um {@code TOMBSTONE} é inserido para preservar a hash-chain. Obrigatório quando {@code enabled = true}. */
    private Integer ttlDays;

    @PostConstruct
    public void validate() {
        if (enabled && ttlDays == null) {
            throw new IllegalStateException("scos.audit.retention.ttl-days is required when retention is enabled");
        }
    }

}
