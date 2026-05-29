
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

import java.net.URI;
import java.nio.file.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies the web advice renders {@link AccessDeniedException} as a 403
 * {@link ProblemDetail} with the access-denied type/title/code.
 */
@ExtendWith(MockitoExtension.class)
class ExceptionsHandlerAccessDeniedTest {

    @Mock LocaleService localeService;
    @InjectMocks ExceptionsHandler handler;

    @Test
    @DisplayName("AccessDeniedException -> 403 ProblemDetail")
    void accessDeniedBecomes403() {
        when(localeService.getMessage(anyString(), any(Object[].class)))
                .thenReturn("Você não tem permissão para acessar este recurso.");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");

        ResponseEntity<ProblemDetail> response =
                handler.handleAccessDeniedException(new AccessDeniedException("/api/admin/users"), request);
        ProblemDetail problem = response.getBody();

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(problem);
        assertEquals(403, problem.getStatus());
        assertEquals("Access Denied", problem.getTitle());
        assertEquals(URI.create("https://docs.sawcunhaos.com.br/problems/scos-004"), problem.getType());
        assertEquals("SCOS-004", problem.getProperties().get("code"));
    }
}
