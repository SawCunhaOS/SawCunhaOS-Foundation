package br.com.sawcunhaos.foundation.jdempotent.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
public class ConfigUtility {
    @Value("${scos.jdempotent.cryptography.algorithm:md5}")
    private String algorithm;
}
