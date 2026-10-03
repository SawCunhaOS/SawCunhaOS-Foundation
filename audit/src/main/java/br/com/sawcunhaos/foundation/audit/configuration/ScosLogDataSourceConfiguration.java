
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

/**
 * Autoconfiguração do datasource <b>isolado</b> da trilha de auditoria ({@code spring.datasource.audit.*}).
 *
 * <p>Cria um pool Hikari próprio, um {@code EntityManagerFactory} restrito ao pacote
 * {@code domain.entity} e um {@code JpaTransactionManager} dedicado
 * ({@code ScosAuditLogTransactionManager}), de modo que a gravação da auditoria nunca participa
 * da transação de negócio. Os repositórios do pacote {@code domain.repository} são ligados a esses
 * beans via {@code @EnableJpaRepositories}. Ativo apenas com {@code scos.audit.enabled=true}.
 *
 * @since 1.2.0
 */
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

    /**
     * Propriedades de conexão do datasource de auditoria, lidas de {@code spring.datasource.audit.*}.
     *
     * @return propriedades de conexão (url, usuário, senha, driver)
     */
    @Bean(name="ScosAuditLogDataSourceProps")
    @ConfigurationProperties("spring.datasource.audit")
    public DataSourceProperties insideAuditLogDataSourceProps() {
        return new DataSourceProperties();
    }

    /**
     * Constrói o pool Hikari do datasource de auditoria.
     *
     * <p>Os parâmetros do pool vêm de {@link ScosAuditHikariConfigProperties}. O pool nasce com
     * {@code autoCommit=false} e {@code isolateInternalQueries=true}, além de propriedades fixas do
     * driver PostgreSQL (cache de prepared statements, {@code reWriteBatchedInserts} para acelerar o
     * {@code saveAll} em lote, {@code socketTimeout}=30 s e {@code loginTimeout}=10 s). O
     * {@code leakDetectionThreshold} só é aplicado quando maior que zero e as métricas Hikari só são
     * registradas se houver um {@code MeterRegistry} no contexto.
     *
     * @param properties propriedades de conexão ({@code spring.datasource.audit})
     * @return datasource de auditoria
     */
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

    /**
     * Fábrica de {@code EntityManager} do datasource de auditoria, restrita às entidades de
     * {@code domain.entity} ({@code ScosAuditLog} e {@code ScosAuditDlqLog}).
     *
     * @param builder   builder do Spring Boot
     * @param dataSource datasource de auditoria
     * @return fábrica da unidade de persistência {@code ScosAuditLog}
     */
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

    /**
     * Gerenciador de transações dedicado à auditoria; é o referenciado por
     * {@code @Transactional("ScosAuditLogTransactionManager")} em todo o módulo.
     *
     * @param entityManagerFactory fábrica de auditoria
     * @return gerenciador de transações JPA da auditoria
     */
    @Bean(name = "ScosAuditLogTransactionManager")
    @ConfigurationProperties("spring.jpa")
    public PlatformTransactionManager insideAuditLogTransactionManager(
            @Qualifier("ScosAuditLogEntityManager") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
