package br.com.sawcunhaos.foundation.audit.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.datasource.audit.hikari")
@Data
public class ScosAuditHikariConfigProperties {

    private int maximumPoolSize;
    private int minimumIdle;
    private long idleTimeout;
    private long maxLifetime;
    private long connectionTimeout;
    private String poolName;
    private boolean registerMbeans = true;
    private boolean autoCommit = false;
    private int leakDetectionThreshold = 20000;
    private String connectionTestQuery = "SELECT 1";
    private int validationTimeout = 5000;
    private int healthCheckInterval = 30000;
    private int KeepaliveTime = 0;


}
