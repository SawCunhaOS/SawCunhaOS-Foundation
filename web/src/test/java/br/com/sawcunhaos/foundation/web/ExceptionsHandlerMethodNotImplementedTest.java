
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
import br.com.sawcunhaos.foundation.core.exception.MethodNotImplementedException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies the web advice renders {@link MethodNotImplementedException} as a
 * 501 {@link ProblemDetail} with the not-implemented type/title/code.
 */
@ExtendWith(MockitoExtension.class)
class ExceptionsHandlerMethodNotImplementedTest {

    @Mock LocaleService localeService;
    @InjectMocks ExceptionsHandler handler;

    @Test
    @DisplayName("MethodNotImplementedException -> 501 ProblemDetail")
    void methodNotImplementedBecomes501() {
        when(localeService.getMessage(anyString()))
                .thenReturn("Funcionalidade não implementada.");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/anything");

        ResponseEntity<ProblemDetail> response =
                handler.handleMethodNotImplementedException(new MethodNotImplementedException(), request);
        ProblemDetail problem = response.getBody();

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
        assertNotNull(problem);
        assertEquals(501, problem.getStatus());
        assertEquals("Not Implemented", problem.getTitle());
        assertEquals(URI.create("https://docs.sawcunhaos.com.br/problems/scos-010"), problem.getType());
        assertEquals("SCOS-010", problem.getProperties().get("code"));
        assertEquals("Funcionalidade não implementada.", problem.getDetail());
        assertEquals("/api/anything", problem.getInstance().toString());
    }
}
