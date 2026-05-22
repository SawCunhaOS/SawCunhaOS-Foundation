
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
