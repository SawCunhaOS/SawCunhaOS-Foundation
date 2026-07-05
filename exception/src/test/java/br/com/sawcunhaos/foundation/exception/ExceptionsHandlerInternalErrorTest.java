
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies the generic fallback: any unhandled exception becomes a 500
 * {@link ProblemDetail} titled "Internal Server Error" that never leaks the
 * stack trace into {@code detail}.
 */
@ExtendWith(MockitoExtension.class)
class ExceptionsHandlerInternalErrorTest {

    @Mock LocaleService localeService;
    @InjectMocks ExceptionsHandler handler;

    @Test
    @DisplayName("unhandled exception -> 500 ProblemDetail without stack trace")
    void unhandledExceptionBecomes500() {
        when(localeService.getMessage(anyString(), any(Object[].class)))
                .thenReturn("Ocorreu um erro interno.");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/anything");
        Exception ex = new RuntimeException("SECRET_INTERNAL_DETAIL at com.foo.Bar");

        ResponseEntity<ProblemDetail> response = handler.handleGenericException(ex, request);
        ProblemDetail problem = response.getBody();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(problem);
        assertEquals(500, problem.getStatus());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals("Ocorreu um erro interno.", problem.getDetail());
        assertFalse(problem.getDetail().contains("SECRET_INTERNAL_DETAIL"));
        assertEquals("SCOS-003", problem.getProperties().get("code"));
    }
}
