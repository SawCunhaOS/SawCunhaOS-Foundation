
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

package br.com.sawcunhaos.foundation.utils.configuration.rest.filter;


import br.com.sawcunhaos.foundation.utils.configuration.rest.filter.properties.ScosFilterProperties;
import br.com.sawcunhaos.foundation.utils.lgpd.SanitizationBodyComponent;
import br.com.sawcunhaos.foundation.utils.lgpd.SanitizationHeadersComponent;
import br.com.sawcunhaos.foundation.utils.utils.DateUtils;
import br.com.sawcunhaos.foundation.utils.utils.IpAddressExtractor;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.ServletServerHttpRequest;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@Order(0)
@RequiredArgsConstructor
public class LoggingInitialFilter implements Filter {

	private final ScosFilterProperties scosFilterProperties;
	private final SanitizationHeadersComponent sanitizationHeadersComponent;
	private final SanitizationBodyComponent sanitizationBodyComponent;
    private final IpAddressExtractor ipExtractor;

	@Override
	public void doFilter(
			ServletRequest request,
			ServletResponse response,
			FilterChain chain
	) throws IOException, ServletException {

		final MultiReadHttpServletRequest req = new MultiReadHttpServletRequest((HttpServletRequest) request);

		if(req.getRequestURI().contains(scosFilterProperties.getURI())) {
			String headers = getRequestHeaders(req);
			String body = getRequestBody(req);
            MDC.put("X-Request-ID", getXRequestId(req));
            MDC.put("IS_IP", getClientIp(req));
            MDC.put("Request-Time", DateUtils.returnDateCurrent());
            MDC.put("Request-Method", req.getMethod());
            MDC.put("Request-URI", createURI(req));
            MDC.put("Request-Content-Type", req.getContentType());
            MDC.put("Headers", headers);
            MDC.put("Request-Body", body);
            log.info("Initial API Call");
            MDC.remove("Request-Time");
            MDC.remove("Request-Method");
            MDC.remove("Request-URI");
            MDC.remove("Content-Type");
            MDC.remove("Headers");
            MDC.remove("Body");
		}

		chain.doFilter(req, response);
	}

	private String createURI(MultiReadHttpServletRequest servletRequest) {
		String queryParams = servletRequest.getParameterMap().entrySet().stream()
				.map(query -> "%s=%s".formatted(query.getKey(), String.join(",", query.getValue()))
		).collect(Collectors.joining("&"));

		if(!queryParams.isEmpty()){
			queryParams = "?%s".formatted(queryParams);
		}

		return "%s%s".formatted(servletRequest.getRequestURI(), queryParams);
	}

	private String getRequestHeaders(MultiReadHttpServletRequest servletRequest) {
		String resquestHeaders = "Headers view not enabled";
		if(scosFilterProperties.isShowRequestHeaders()) {
			resquestHeaders = sanitizationHeadersComponent.sanitizeHeader(new ServletServerHttpRequest(servletRequest).getHeaders());
		}
		return resquestHeaders;
	}

    private String getXRequestId(MultiReadHttpServletRequest servletRequest) {
        String xRequestId = servletRequest.getHeader("X-Request-ID");
        if (xRequestId == null || xRequestId.isEmpty()) {
            xRequestId = UUID.randomUUID().toString();
        }
        return xRequestId;
    }

    private String getClientIp(MultiReadHttpServletRequest servletRequest) {
        return ipExtractor.extractClientIp(servletRequest);
    }

	private String getRequestBody(MultiReadHttpServletRequest servletRequest) throws IOException {
		String responseBody = "Body view not enabled";
		if(scosFilterProperties.isShowRequestBody()) {
			responseBody = sanitizationBodyComponent.sanitizeBody(servletRequest.getInputStream());
		}
		return responseBody;
	}
}
