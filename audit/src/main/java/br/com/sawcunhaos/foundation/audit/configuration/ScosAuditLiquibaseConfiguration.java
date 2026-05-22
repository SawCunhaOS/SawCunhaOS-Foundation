
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

import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditLiquibaseProperties;
import liquibase.integration.spring.MultiTenantSpringLiquibase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ScosAuditLiquibaseProperties.class})
@RequiredArgsConstructor
public final class ScosAuditLiquibaseConfiguration {

    private final ScosAuditLiquibaseProperties liquibaseProperties;

    @Bean("ScosAuditLiquibase")
    @Primary
    @DependsOn("ScosAuditLogDataSource")
    public MultiTenantSpringLiquibase scosAuditLiquibase(@Qualifier("ScosAuditLogDataSource") DataSource dataSource) {
        MultiTenantSpringLiquibase liquibase = new MultiTenantSpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(liquibaseProperties.getChangeLog());
        liquibase.setContexts(liquibaseProperties.getContexts());
        liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
        liquibase.setDropFirst(liquibaseProperties.isDropFirst());
        liquibase.setShouldRun(liquibaseProperties.isEnabled());
        liquibase.setRollbackFile(liquibaseProperties.getRollbackFile());
        liquibase.setLiquibaseTablespace(liquibaseProperties.getLiquibaseTablespace());
        liquibase.setDatabaseChangeLogTable(liquibaseProperties.getDatabaseChangeLogTable());
        liquibase.setDatabaseChangeLogLockTable(liquibaseProperties.getDatabaseChangeLogLockTable());
        liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
        return liquibase;
    }
}
