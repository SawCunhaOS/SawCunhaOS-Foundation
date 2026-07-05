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
 * Properties de imutabilidade da trilha de auditoria ({@code scos.audit.immutability.*}).
 *
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "scos.audit.immutability")
@Data
public class ScosAuditImmutabilityProperties {

    /**
     * Habilita a hash-chain SHA-256 para tamper-evidence (opt-in).
     *
     * <p>Quando {@code true}, cada evento recebe um hash SHA-256 calculado sobre seus
     * metadados e o hash do evento anterior do mesmo registro. Adulteração ou remoção
     * de qualquer evento quebra a cadeia, detectável via
     * {@code ScosAuditIntegrityService#verifyChain}. Padrão: {@code false}.
     */
    private boolean hashChain = false;

}
