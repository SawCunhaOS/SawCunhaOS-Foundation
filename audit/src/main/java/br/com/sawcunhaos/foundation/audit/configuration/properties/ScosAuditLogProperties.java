
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "scos.audit")
@AutoConfiguration
@Data
public class ScosAuditLogProperties {

    @Value("${scos.audit.system:SFA_AUDIT}")
    private String system;

    @Value("${scos.audit.enabled:false}")
    private boolean enable;

    @Value("${scos.audit.liquibase.enabled:true}")
    private boolean enableLiquibase;
}
