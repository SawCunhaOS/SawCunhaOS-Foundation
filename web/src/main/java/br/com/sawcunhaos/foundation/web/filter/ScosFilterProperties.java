
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

/**
 * Propriedades dos filtros de log ({@link LoggingInitialFilter}, {@link LoggingFinalFilter}),
 * recarregáveis em runtime ({@code @RefreshScope}, via {@code spring-cloud-context}).
 *
 * <ul>
 *   <li>{@code server.servlet.context-path} (padrão {@code /}): compõe o prefixo monitorado.</li>
 *   <li>{@code server.filter.show-request-body} (padrão {@code false}): loga o corpo da requisição.</li>
 *   <li>{@code server.filter.show-request-headers} (padrão {@code false}): loga os cabeçalhos.</li>
 *   <li>{@code server.filter.show-response-body} (padrão {@code false}): loga o corpo da resposta.</li>
 * </ul>
 *
 * <p>Corpo e cabeçalhos passam pela sanitização do módulo {@code privacy} antes de ir ao log.</p>
 *
 * @since 1.2.0
 */
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

    /**
     * @return prefixo de URI cujas requisições são logadas: {@code /api}, ou
     *         {@code <context-path>/api} quando há context-path. A checagem nos filtros usa
     *         {@code contains}, não {@code startsWith}.
     */
    public String getURI(){
        if (getContextPath().equals("/")) return API;
        return getContextPath()+API;
    }
}
