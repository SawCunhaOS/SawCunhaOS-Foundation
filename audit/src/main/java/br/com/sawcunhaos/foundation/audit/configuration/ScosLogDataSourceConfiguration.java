
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

import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditHikariConfigProperties;
import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditLogProperties;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@ConditionalOnProperty(prefix="scos.audit", name = "enabled", havingValue = "true")
@AutoConfiguration
@EnableJpaRepositories(
        basePackages = "br.com.sawcunhaos.foundation.audit.domain.repository",
        entityManagerFactoryRef = "ScosAuditLogEntityManager",
        transactionManagerRef = "ScosAuditLogTransactionManager"
)
@EnableTransactionManagement
@RequiredArgsConstructor
@EnableConfigurationProperties({ScosAuditHikariConfigProperties.class})
public class ScosLogDataSourceConfiguration {

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private final ScosAuditHikariConfigProperties scosAuditHikariConfigProperties;

    @Bean(name="ScosAuditLogDataSourceProps")
    @ConfigurationProperties("spring.datasource.audit")
    public DataSourceProperties insideAuditLogDataSourceProps() {
        return new DataSourceProperties();
    }

    @Bean(name="ScosAuditLogDataSource")
    public DataSource insideAuditLogDataSource(@Qualifier("ScosAuditLogDataSourceProps") DataSourceProperties properties){
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        dataSource.setMetricRegistry(meterRegistry);
        dataSource.setRegisterMbeans(scosAuditHikariConfigProperties.isRegisterMbeans());
        dataSource.setMaximumPoolSize(scosAuditHikariConfigProperties.getMaximumPoolSize());
        dataSource.setMinimumIdle(scosAuditHikariConfigProperties.getMinimumIdle());
        dataSource.setIdleTimeout(scosAuditHikariConfigProperties.getIdleTimeout());
        dataSource.setMaxLifetime(scosAuditHikariConfigProperties.getMaxLifetime());
        dataSource.setConnectionTimeout(scosAuditHikariConfigProperties.getConnectionTimeout());
        dataSource.setPoolName(scosAuditHikariConfigProperties.getPoolName());
        dataSource.setValidationTimeout(scosAuditHikariConfigProperties.getValidationTimeout());

        dataSource.setKeepaliveTime(scosAuditHikariConfigProperties.getKeepaliveTime());
        dataSource.setIsolateInternalQueries(true);
        dataSource.setAutoCommit(false);

        if (scosAuditHikariConfigProperties.getLeakDetectionThreshold() > 0) {
            dataSource.setLeakDetectionThreshold(scosAuditHikariConfigProperties.getLeakDetectionThreshold());
        }

        // Configurações do driver JDBC
        dataSource.addDataSourceProperty("cachePrepStmts", "true");
        dataSource.addDataSourceProperty("prepStmtCacheSize", "1000");
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");
        dataSource.addDataSourceProperty("useServerPrepStmts", "true");
        dataSource.addDataSourceProperty("reWriteBatchedInserts", "true"); // Batch rewriting
        dataSource.addDataSourceProperty("defaultRowFetchSize", "100");    // Fetch size otimizado
        dataSource.addDataSourceProperty("tcpKeepAlive", "true");         // Keep-alive TCP
        dataSource.addDataSourceProperty("loginTimeout", "10");            // Timeout de login (10s)
        dataSource.addDataSourceProperty("socketTimeout", "30");           // Socket timeout (30s)

        return dataSource;
    }

    @Bean(name="ScosAuditLogEntityManager")
    public LocalContainerEntityManagerFactoryBean insideAuditLogEntityManager(
            EntityManagerFactoryBuilder builder,
            @Qualifier("ScosAuditLogDataSource") DataSource dataSource
    ){
        return builder.dataSource(dataSource)
                .packages("br.com.sawcunhaos.foundation.audit.domain.entity")
                .persistenceUnit("ScosAuditLog")
                .build();
    }

    @Bean(name = "ScosAuditLogTransactionManager")
    @ConfigurationProperties("spring.jpa")
    public PlatformTransactionManager insideAuditLogTransactionManager(
            @Qualifier("ScosAuditLogEntityManager") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
