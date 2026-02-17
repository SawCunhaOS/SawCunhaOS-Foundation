package br.com.sawcunhaos.foundation.security.configuration.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Data
@Configuration
@RefreshScope
public class CorsProperties {

    private final String COMMA_REGEX = ",";

    @Value("${cors-security.allowOrigin:*}")
    private String allowOrigin;
    @Value("${cors-security.allowMethods:GET,POST,DELETE,PUT,OPTIONS}")
    private String allowMethods;
    @Value("${cors-security.allowHeaders:*}")
    private String allowHeaders;
    @Value("${cors-security.allowCredentials:true}")
    private String allowCredentials;
    @Value("${cors-security.maxAge:1800}")
    private String maxAge;

    public List<String> allowOrigin() {
        return Arrays.stream(allowOrigin.split(COMMA_REGEX)).toList();
    }

    public List<String> allowMethods() {
        return Arrays.stream(allowMethods.split(COMMA_REGEX)).toList();
    }

    public List<String> allowHeaders() {
        return Arrays.stream(allowHeaders.split(COMMA_REGEX)).toList();
    }

    public Long maxAge() {
        return Long.parseLong(maxAge);
    }

    public Boolean allowCredentials() {
        return Boolean.parseBoolean(allowCredentials);
    }
}
