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
