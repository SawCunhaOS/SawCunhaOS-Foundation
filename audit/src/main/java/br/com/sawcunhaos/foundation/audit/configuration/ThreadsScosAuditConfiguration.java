
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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Pool de threads do pipeline de auditoria; ativo apenas com {@code scos.audit.enabled=true}.
 *
 * @since 1.2.0
 */
@ConditionalOnProperty(prefix="scos.audit", name = "enabled", havingValue = "true")
@Configuration(proxyBeanMethods = false)
public final class ThreadsScosAuditConfiguration {

    /**
     * Scheduler baseado em threads virtuais (prefixo {@code vt-audit-sch-}, tamanho 30) usado pelos
     * métodos {@code @Async("ScosAuditLogAsyncExecutor")} que montam e enfileiram os eventos. Por ser o
     * único {@code TaskScheduler} do módulo, é também o que o Spring usa para os métodos
     * {@code @Scheduled} do módulo.
     *
     * @return scheduler de auditoria
     */
    @Bean(name = "ScosAuditLogAsyncExecutor")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(30);
        scheduler.setThreadNamePrefix("vt-audit-sch-");
        scheduler.setVirtualThreads(true);
        return scheduler;
    }
}
