package br.com.sawcunhaos.foundation.security.configuration.database;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;


@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "ScosSecurityEntityManagerFactory",
        transactionManagerRef = "ScosSecurityTransactionManager",
        basePackages = {
                "br.com.sawcunhaos.foundation.security.domain.repository",
        })
@EnableConfigurationProperties({ScosSecurityHikariConfigProperties.class})
@RequiredArgsConstructor
public final class ScosSecurityDataSourceConfiguration {

    @Autowired(required = false)
    private MeterRegistry meterRegistry;
    private final ScosSecurityHikariConfigProperties insideFlowHikariConfigProperties;

    @Bean(name = "ScosSecurityDataSourcePropos")
    @ConfigurationProperties("scos.security.datasource")
    public DataSourceProperties insideFlowSecurityDataSourcePropos() {
        return new DataSourceProperties();
    }

    @Bean(name = "ScosSecurityDataSource")
    public DataSource insideFlowSecurityDataSource(@Qualifier("ScosSecurityDataSourcePropos") DataSourceProperties properties) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        dataSource.setMetricRegistry(meterRegistry);
        dataSource.setRegisterMbeans(insideFlowHikariConfigProperties.isRegisterMbeans());
        dataSource.setMaximumPoolSize(insideFlowHikariConfigProperties.getMaximumPoolSize());
        dataSource.setMinimumIdle(insideFlowHikariConfigProperties.getMinimumIdle());
        dataSource.setIdleTimeout(insideFlowHikariConfigProperties.getIdleTimeout());
        dataSource.setMaxLifetime(insideFlowHikariConfigProperties.getMaxLifetime());
        dataSource.setConnectionTimeout(insideFlowHikariConfigProperties.getConnectionTimeout());
        dataSource.setPoolName(insideFlowHikariConfigProperties.getPoolName());
        dataSource.setAutoCommit(insideFlowHikariConfigProperties.isAutoCommit());
        dataSource.setLeakDetectionThreshold(insideFlowHikariConfigProperties.getLeakDetectionThreshold());
        dataSource.setConnectionTestQuery(insideFlowHikariConfigProperties.getConnectionTestQuery());
        dataSource.setValidationTimeout(insideFlowHikariConfigProperties.getValidationTimeout());

        // Configurações do driver JDBC
        dataSource.addDataSourceProperty("cachePrepStmts", "true");
        dataSource.addDataSourceProperty("prepStmtCacheSize", "1000");
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");
        dataSource.addDataSourceProperty("useServerPrepStmts", "true");

        return dataSource;
    }

    @Bean(name = "ScosSecurityEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean ScosSecurityEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("ScosSecurityDataSource") DataSource dataSource
    ) {
        return builder.dataSource(dataSource)
                .packages(
                        "br.com.sawcunhaos.foundation.security.domain.entity"
                )
                .persistenceUnit("ScosSecurityPersistenceUnit")
                .build();
    }

    @Bean(name = "ScosSecurityTransactionManager")
    public PlatformTransactionManager ScosSecurityTransactionManager(
            @Qualifier("ScosSecurityEntityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
