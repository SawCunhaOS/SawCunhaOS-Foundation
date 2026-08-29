
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
import br.com.sawcunhaos.foundation.web.support.DomainErrorController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the fixed-status domain paths (403/400/501/500) through real MVC dispatch
 * — content negotiation, {@code @ControllerAdvice} resolution and Jackson 3 serialization
 * included — instead of the direct, mocked method calls every other {@code ExceptionsHandler*Test}
 * uses. Mirrors {@link ExceptionsHandlerHandleExceptionInternalTest}, which already does this
 * for the native 404/405/406 cases.
 */
@WebMvcTest(controllers = DomainErrorController.class)
@Import({ExceptionsHandler.class, DomainErrorController.class})
class ExceptionsHandlerDomainDispatchTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	LocaleService localeService;

	@Test
	@DisplayName("AccessDeniedException (403) -> ScosProblemDetails via real dispatch")
	void accessDenied() throws Exception {
		when(localeService.getMessage(anyString(), any(Object[].class))).thenReturn("Acesso negado");

		mockMvc.perform(get("/forbidden"))
				.andExpect(status().isForbidden())
				.andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/problem+json")))
				.andExpect(jsonPath("$.code").value("SCOS-004"))
				.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	@DisplayName("MethodNotImplementedException (501) -> ScosProblemDetails via real dispatch")
	void notImplemented() throws Exception {
		when(localeService.getMessage(anyString(), any(Object[].class))).thenReturn("Nao implementado");

		mockMvc.perform(get("/legacy"))
				.andExpect(status().isNotImplemented())
				.andExpect(jsonPath("$.code").value("SCOS-010"))
				.andExpect(jsonPath("$.status").value(501));
	}

	@Test
	@DisplayName("Unhandled exception (500) -> ScosProblemDetails via real dispatch")
	void generic() throws Exception {
		when(localeService.getMessage(anyString(), any(Object[].class))).thenReturn("Erro interno");

		mockMvc.perform(get("/boom"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("SCOS-003"))
				.andExpect(jsonPath("$.status").value(500));
	}

	@Test
	@DisplayName("@RequestParam @Min violation (400) -> ScosProblemDetails via real dispatch")
	void simpleParamValidation() throws Exception {
		when(localeService.getMessage(anyString(), any(Object[].class))).thenReturn("Parametro invalido");

		mockMvc.perform(get("/paged").param("page", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("SCOS-001"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors").isArray());
	}
}
