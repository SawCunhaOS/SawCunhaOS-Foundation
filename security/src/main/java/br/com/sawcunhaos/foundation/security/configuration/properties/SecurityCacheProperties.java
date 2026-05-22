package br.com.sawcunhaos.foundation.security.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scos.security.cache")
@Data
public class SecurityCacheProperties {

    private int maximumSizeLogins = 1000;
    private int maximumSizePermission = 5000;
    private long expireAfterWriteLogin = 2;
    private long expireAfterAccessLogin = 1;
    private long expireAfterWritePermission = 10;

}
