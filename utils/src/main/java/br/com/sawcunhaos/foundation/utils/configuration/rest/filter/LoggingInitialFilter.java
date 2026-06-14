
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


import br.com.sawcunhaos.foundation.privacy.SanitizationBodyComponent;
import br.com.sawcunhaos.foundation.privacy.SanitizationHeadersComponent;
import br.com.sawcunhaos.foundation.utils.configuration.rest.filter.properties.ScosFilterProperties;
import br.com.sawcunhaos.foundation.utils.utils.DateUtils;
import br.com.sawcunhaos.foundation.utils.utils.IpAddressExtractor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * First filter in the chain ({@code @Order(0)}). Establishes per-request MDC
 * context and the initial request log.
 *
 * <p>Extends {@link OncePerRequestFilter} so it does not run twice when the
 * container forwards/includes (e.g. Spring's forward to {@code /error}).</p>
 *
 * <p>MDC is split in two: {@code X-Request-ID} and {@code IS_IP} are populated
 * for every request regardless of URI (downstream error handlers need the
 * correlation id even on {@code /actuator}, {@code /health}, etc.), and the
 * {@code X-Request-ID} is echoed back in the response header. The verbose
 * request/response logging stays conditional on the configured URI prefix.</p>
 *
 * <p>PII masking is delegated to the {@code scos-foundation-privacy} sanitization
 * components; this filter only adapts the web types (headers, body stream) to them.</p>
 *
 * <p>As the outermost filter, it owns the single {@code MDC.clear()} in a
 * {@code finally} block, guaranteeing cleanup even when the chain throws and
 * preventing context leaking across pooled threads.</p>
 */
@Slf4j
@Configuration
@Order(0)
@RequiredArgsConstructor
public class LoggingInitialFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_HEADER = "X-Request-ID";

	private final ScosFilterProperties scosFilterProperties;
	private final SanitizationHeadersComponent sanitizationHeadersComponent;
	private final SanitizationBodyComponent sanitizationBodyComponent;
    private final IpAddressExtractor ipExtractor;

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

		final MultiReadHttpServletRequest req = new MultiReadHttpServletRequest(request);

		try {
			MDC.put(REQUEST_ID_HEADER, resolveRequestId(req));
			MDC.put("IS_IP", getClientIp(req));
			response.setHeader(REQUEST_ID_HEADER, MDC.get(REQUEST_ID_HEADER));

			if (req.getRequestURI().contains(scosFilterProperties.getURI())) {
				String headers = getRequestHeaders(req);
				String body = getRequestBody(req);
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
				MDC.remove("Request-Content-Type");
				MDC.remove("Headers");
				MDC.remove("Request-Body");
			}

			chain.doFilter(req, response);
		} finally {
			MDC.clear();
		}
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
		if (!scosFilterProperties.isShowRequestHeaders()) {
			return "Headers view not enabled";
		}
		final HttpHeaders headers = new ServletServerHttpRequest(servletRequest).getHeaders();
		final Map<String, String> raw = new LinkedHashMap<>();
		headers.headerNames().forEach(name -> {
			final List<String> values = headers.get(name);
			raw.put(name, (values != null && !values.isEmpty()) ? values.get(0) : "");
		});

		final Map<String, String> masked = sanitizationHeadersComponent.sanitize(raw);
		final StringBuilder formatted = new StringBuilder();
		masked.forEach((name, value) -> formatted
				.append("Header Name -> ").append(name).append(" -- ").append(value).append('\n'));
		return formatted.toString();
	}

    /**
     * Resolves the correlation id: reuses the inbound {@code X-Request-ID} header
     * when the client supplies one, otherwise generates a fresh UUID v4.
     */
    private String resolveRequestId(HttpServletRequest servletRequest) {
        String xRequestId = servletRequest.getHeader(REQUEST_ID_HEADER);
        if (xRequestId == null || xRequestId.isEmpty()) {
            xRequestId = UUID.randomUUID().toString();
        }
        return xRequestId;
    }

    private String getClientIp(HttpServletRequest servletRequest) {
        return ipExtractor.extractClientIp(servletRequest);
    }

	private String getRequestBody(MultiReadHttpServletRequest servletRequest) throws IOException {
		if (!scosFilterProperties.isShowRequestBody()) {
			return "Body view not enabled";
		}
		return sanitizeBody(servletRequest.getInputStream());
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
