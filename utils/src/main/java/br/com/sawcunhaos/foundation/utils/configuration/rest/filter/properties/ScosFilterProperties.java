package br.com.sawcunhaos.foundation.utils.configuration.rest.filter.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@RefreshScope
public class ScosFilterProperties {

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Value("${server.filter.show-request-body:false}")
    private boolean showRequestBody;

    @Value("${server.filter.show-request-headers:false}")
    private boolean showRequestHeaders;

    @Value("${server.filter.show-response-body:false}")
    private boolean showResponseBody;

    private static final String API = "/api";

    public String getURI(){
        if (getContextPath().equals("/")) return API;
        return getContextPath()+API;
    }
}
