
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

package br.com.sawcunhaos.foundation.web;

import br.com.sawcunhaos.foundation.core.specification.LocaleService;
import br.com.sawcunhaos.foundation.web.support.PingController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Story 2.7 (AC #1): confirms that exceptions Spring's {@code ResponseEntityExceptionHandler}
 * would otherwise render in its own native format — unmatched route
 * ({@code NoResourceFoundException}, requires {@code spring.mvc.throw-exception-if-no-handler-found=true}
 * on the host application, see the root {@code README.md} "SCOS Foundation Web" section),
 * unsupported HTTP method
 * ({@code HttpRequestMethodNotSupportedException}) and unacceptable {@code Accept} header
 * ({@code HttpMediaTypeNotAcceptableException}) — instead come back as a
 * {@link br.com.sawcunhaos.foundation.web.model.ScosProblemDetails}, once
 * {@code ExceptionsHandler#handleExceptionInternal} is overridden.
 *
 * <p>{@code @WebMvcTest} (not {@code MockMvcBuilders.standaloneSetup()}) is required: the
 * {@code throw-exception-if-no-handler-found} property is only consumed by Boot's
 * {@code WebMvcAutoConfiguration}, which a standalone setup never runs.</p>
 */
@WebMvcTest(controllers = PingController.class)
@Import({ExceptionsHandler.class, PingController.class})
@TestPropertySource(properties = "spring.mvc.throw-exception-if-no-handler-found=true")
class ExceptionsHandlerHandleExceptionInternalTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	LocaleService localeService;

	@Test
	@DisplayName("Unmatched route (404) -> ScosProblemDetails, not Spring's native error body")
	void unmatchedRouteReturnsScosProblemDetails() throws Exception {
		mockMvc.perform(get("/does-not-exist"))
				.andExpect(status().isNotFound())
				.andExpect(header().string("Content-Type", containsString("application/problem+json")))
				.andExpect(jsonPath("$.code").value("SCOS-003"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	@DisplayName("Unsupported HTTP method (405) -> ScosProblemDetails, not Spring's native error body, Allow header preserved")
	void unsupportedMethodReturnsScosProblemDetails() throws Exception {
		mockMvc.perform(post("/ping"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code").value("SCOS-003"))
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(header().string("Allow", containsString("GET")));
	}

	@Test
	@DisplayName("Unacceptable media type (406) -> ScosProblemDetails, not Spring's native error body")
	void unacceptableMediaTypeReturnsScosProblemDetails() throws Exception {
		mockMvc.perform(get("/ping").accept(MediaType.APPLICATION_XML))
				.andExpect(status().isNotAcceptable())
				.andExpect(jsonPath("$.code").value("SCOS-003"))
				.andExpect(jsonPath("$.status").value(406))
				.andExpect(jsonPath("$.timestamp").exists());
	}
}
