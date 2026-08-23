
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

package br.com.sawcunhaos.foundation.web.filter;

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
