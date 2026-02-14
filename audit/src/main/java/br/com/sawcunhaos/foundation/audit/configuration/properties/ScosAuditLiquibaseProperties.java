package br.com.sawcunhaos.foundation.audit.configuration.properties;

import br.com.sawcunhaos.foundation.utils.configuration.liquibase.BaseLiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "liquibase.scos-audit")
public class ScosAuditLiquibaseProperties extends BaseLiquibaseProperties {

}
