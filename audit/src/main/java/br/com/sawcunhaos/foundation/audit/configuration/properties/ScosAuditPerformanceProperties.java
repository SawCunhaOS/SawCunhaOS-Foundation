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
 * Properties de tuning de performance do pipeline de auditoria ({@code scos.audit.performance.*}).
 *
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "scos.audit.performance")
@Data
public class ScosAuditPerformanceProperties {

    /** Capacidade máxima da fila em memória (número de eventos). Quando atingida, novos eventos são roteados diretamente à DLQ. Padrão: {@code 10000}. */
    private int queueCapacity = 10000;

    /** Número máximo de eventos por operação {@code saveAll} ao persistir um lote. Padrão: {@code 100}. */
    private int batchSize = 100;

    /** Intervalo em milissegundos entre cada ciclo de flush da fila para o banco. O flush ocorre mesmo que o lote não esteja completo. Padrão: {@code 500} ms. */
    private long flushIntervalMs = 500L;

}
