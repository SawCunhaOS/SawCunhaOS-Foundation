
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

import br.com.sawcunhaos.foundation.jpa.liquibase.BaseLiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do Liquibase de auditoria ({@code scos.audit.liquibase.*}); herda o contrato de
 * {@code BaseLiquibaseProperties} do módulo {@code jpa}.
 *
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "scos.audit.liquibase")
public class ScosAuditLiquibaseProperties extends BaseLiquibaseProperties {

}
