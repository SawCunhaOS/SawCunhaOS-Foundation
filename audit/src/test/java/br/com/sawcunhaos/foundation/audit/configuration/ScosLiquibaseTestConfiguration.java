
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

import br.com.sawcunhaos.foundation.audit.service.ScosHibernateAuditListener;
import br.com.sawcunhaos.foundation.audit.specification.ScosAuditService;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import liquibase.integration.spring.MultiTenantSpringLiquibase;
import lombok.RequiredArgsConstructor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@DependsOn({"ScosAuditService", "ScosUserAuthentication"})
public final class ScosLiquibaseTestConfiguration {

    private final ScosAuditService scosAuditService;
    private final ScosUserAuthentication scosUserAuthentication;
    private final EntityManagerFactory entityManagerFactory;

    
    @Bean("ScosAuditLogTestLiquibase")
    @DependsOn("ScosAuditLogDataSource")
    public MultiTenantSpringLiquibase insideAuditLogLiquibase(@Qualifier("ScosAuditLogDataSource") DataSource dataSource) {
        MultiTenantSpringLiquibase liquibase = new MultiTenantSpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:test/db/changelog/db_audit.changelog-master.yaml");
        liquibase.setShouldRun(true);
        return liquibase;
    }

    @PostConstruct
    public void registerListeners() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

        ScosHibernateAuditListener listener = new ScosHibernateAuditListener(scosAuditService, scosUserAuthentication);

        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(listener);
        registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(listener);
        registry.getEventListenerGroup(EventType.POST_DELETE).appendListener(listener);
    }
}
