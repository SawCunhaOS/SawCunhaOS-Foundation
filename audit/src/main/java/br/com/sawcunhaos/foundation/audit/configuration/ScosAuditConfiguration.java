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

package br.com.sawcunhaos.foundation.audit.configuration;

import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditDurabilityProperties;
import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditImmutabilityProperties;
import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditPerformanceProperties;
import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditRetentionProperties;
import br.com.sawcunhaos.foundation.audit.service.ScosAuditQueue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Autoconfiguração principal do módulo de auditoria; ativa apenas com {@code scos.audit.enabled=true}.
 *
 * <p>Habilita agendamento ({@code @EnableScheduling}, usado pelo consumidor de lote, pelo job da DLQ e
 * pelo job de retenção), execução assíncrona ({@code @EnableAsync}), AspectJ, registra os
 * {@code @ConfigurationProperties} de performance, durabilidade, imutabilidade e retenção e expõe a
 * fila em memória. Exclui {@code DataSourceAutoConfiguration}: o único datasource do módulo é o de
 * {@link ScosLogDataSourceConfiguration}.
 *
 * @since 1.2.0
 */
@ConditionalOnProperty(prefix="scos.audit", name = "enabled", havingValue = "true")
@AutoConfiguration
@EnableAspectJAutoProxy
@EnableAsync
@EnableScheduling
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@EnableConfigurationProperties({
        ScosAuditPerformanceProperties.class,
        ScosAuditDurabilityProperties.class,
        ScosAuditImmutabilityProperties.class,
        ScosAuditRetentionProperties.class
})
public class ScosAuditConfiguration {

    /**
     * Fila em memória que desacopla a captura do evento da gravação em banco.
     *
     * @param performanceProperties propriedades de performance (capacidade da fila)
     * @return fila com capacidade {@code scos.audit.performance.queue-capacity}
     */
    @Bean
    public ScosAuditQueue scosAuditQueue(ScosAuditPerformanceProperties performanceProperties) {
        return new ScosAuditQueue(performanceProperties.getQueueCapacity());
    }

}
