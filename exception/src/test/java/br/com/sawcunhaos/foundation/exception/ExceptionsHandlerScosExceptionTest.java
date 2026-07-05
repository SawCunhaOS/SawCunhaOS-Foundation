
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

package br.com.sawcunhaos.foundation.exception;

import br.com.sawcunhaos.foundation.utils.enums.ScosExceptionCode;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies that a business {@link ScosException} renders as an RFC 9457
 * {@link ProblemDetail}: status 400, no {@code data} wrapper, all original data
 * preserved ({@code message}→{@code detail}, {@code codeError}→{@code code}).
 *
 * <p>The {@code application/problem+json} content type is set by Spring's message
 * converter when the body is a {@link ProblemDetail}; it is asserted end-to-end
 * in the security filter tests where serialization is manual.</p>
 */
@ExtendWith(MockitoExtension.class)
class ExceptionsHandlerScosExceptionTest {

    @Mock LocaleService localeService;
    @InjectMocks ExceptionsHandler handler;

    @Test
    @DisplayName("ScosException -> ProblemDetail 400 with code/type/title/detail/instance")
    void businessExceptionBecomesProblemDetail() {
        when(localeService.getMessage(anyString(), any(Object[].class))).thenReturn("CPF informado é inválido.");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/persons");
        ScosException exception = new ScosException(ScosExceptionCode.CPF_INVALID, "cpf");

        ResponseEntity<ProblemDetail> response = handler.handleScosException(exception, request);
        ProblemDetail problem = response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(problem);
        assertEquals(400, problem.getStatus());
        assertEquals("CPF informado é inválido.", problem.getTitle());
        assertEquals("CPF informado é inválido.", problem.getDetail());
        assertEquals(URI.create("https://docs.sawcunhaos.com.br/problems/scos-007"), problem.getType());
        assertEquals("/api/persons", problem.getInstance().toString());
        assertEquals("SCOS-007", problem.getProperties().get("code"));
        assertNotNull(problem.getProperties().get("timestamp"));
    }
}
