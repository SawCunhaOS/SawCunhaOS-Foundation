
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


import br.com.sawcunhaos.foundation.privacy.SanitizationBodyComponent;
import br.com.sawcunhaos.foundation.core.utils.DateUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Final-stage filter ({@code @Order(100)}) that logs the response. Extends
 * {@link OncePerRequestFilter} to avoid double execution on forward/include.
 *
 * <p>PII masking is delegated to the {@code scos-foundation-privacy} body sanitizer; this filter only adapts
 * the response stream to it.</p>
 *
 * <p>It no longer calls {@code MDC.clear()}: cleanup is owned exclusively by the
 * outermost {@link LoggingInitialFilter} ({@code @Order(0)}), whose
 * {@code finally} block guarantees clearing even when the chain throws before
 * reaching this filter.</p>
 */
@Slf4j
@Configuration
@Order(100)
@RequiredArgsConstructor
public class LoggingFinalFilter extends OncePerRequestFilter {

	private final ScosFilterProperties scosFilterProperties;
	private final SanitizationBodyComponent sanitizationBodyComponent;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		// Ignora requisições gRPC — usam I/O async que é incompatível
		// com o MultiReadHttpServletRequest
		String contentType = request.getContentType();
		return contentType != null && contentType.startsWith("application/grpc");
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain chain
	) throws IOException, ServletException {

		ContentCachingResponseWrapper servletResponse = new ContentCachingResponseWrapper(response);

		chain.doFilter(request, servletResponse);
		if(request.getRequestURI().contains(scosFilterProperties.getURI())) {
			String responseBody = getResponseBody(servletResponse);
            MDC.put("Response-Time", DateUtils.returnDateCurrent());
            MDC.put("Response-Code", servletResponse.getStatus() + "");
            MDC.put("Response-Content-Type", servletResponse.getContentType());
            MDC.put("Response-Body", responseBody);
            log.info("Final API Call");
		}
		servletResponse.copyBodyToResponse();
	}

	private String getResponseBody(ContentCachingResponseWrapper servletResponse) {
		if (!scosFilterProperties.isShowResponseBody()) {
			return "Body view not enabled";
		}
		return sanitizeBody(servletResponse.getContentInputStream());
	}

	/**
	 * Reads the stream and applies the privacy body sanitizer; an empty payload is reported with a marker.
	 */
	private String sanitizeBody(final InputStream bodyInputStream) {
		final String body = new BufferedReader(new InputStreamReader(bodyInputStream, StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n"));
		if (body.isEmpty()) {
			return "Does not have Body";
		}
		return sanitizationBodyComponent.sanitize(body);
	}
}
