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

    @Bean
    public ScosAuditQueue scosAuditQueue(ScosAuditPerformanceProperties performanceProperties) {
        return new ScosAuditQueue(performanceProperties.getQueueCapacity());
    }

}
