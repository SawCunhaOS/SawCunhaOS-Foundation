
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

import br.com.sawcunhaos.foundation.exception.model.ScosFieldError;
import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that validation errors render with the {@code errors} array using
 * JSON Pointer (RFC 6901): {@code attribute}→{@code pointer},
 * {@code message}→{@code detail}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExceptionsHandlerValidationTest {

    @Mock LocaleService localeService;
    @InjectMocks ExceptionsHandler handler;

    private MethodArgumentNotValidException exceptionWith(FieldError... fieldErrors) {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        for (FieldError fe : fieldErrors) {
            bindingResult.addError(fe);
        }
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        return ex;
    }

    private FieldError fieldError(String field, String message) {
        return new FieldError("target", field, null, false, null, new Object[0], message);
    }

    @SuppressWarnings("unchecked")
    private List<ScosFieldError> errorsOf(ProblemDetail problem) {
        return (List<ScosFieldError>) problem.getProperties().get("errors");
    }

    @Test
    @DisplayName("simple fields produce #/<field> pointers")
    void simpleFieldsBecomeJsonPointers() {
        when(localeService.getMessage(eq("SCOS-002"), anyList())).thenReturn("deve ser um e-mail válido");
        when(localeService.getMessage(eq("SCOS-003"), anyList())).thenReturn("não deve estar em branco");
        when(localeService.getMessage(eq("SCOS-001"), any(Object[].class))).thenReturn("Um ou mais campos estão inválidos.");

        MethodArgumentNotValidException ex = exceptionWith(
                fieldError("email", "SCOS-002"),
                fieldError("name", "SCOS-003")
        );
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("POST", "/api/users"));

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
        ProblemDetail problem = (ProblemDetail) response.getBody();

        assertNotNull(problem);
        assertEquals(400, problem.getStatus());
        assertEquals("Validation Error", problem.getTitle());
        assertEquals("/api/users", problem.getInstance().toString());
        assertEquals("SCOS-001", problem.getProperties().get("code"));

        List<ScosFieldError> errors = errorsOf(problem);
        assertEquals(2, errors.size());
        assertEquals("#/email", errors.get(0).pointer());
        assertEquals("deve ser um e-mail válido", errors.get(0).detail());
        assertEquals("#/name", errors.get(1).pointer());
    }

    @Test
    @DisplayName("nested field produces #/<path>/<segment> pointer")
    void nestedFieldBecomesNestedPointer() {
        when(localeService.getMessage(anyString(), anyList())).thenReturn("não deve estar em branco");
        when(localeService.getMessage(eq("SCOS-001"), any(Object[].class))).thenReturn("inválido");

        MethodArgumentNotValidException ex = exceptionWith(fieldError("address.street", "SCOS-002"));
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("POST", "/api/users"));

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
        ProblemDetail problem = (ProblemDetail) response.getBody();

        List<ScosFieldError> errors = errorsOf(problem);
        assertEquals(1, errors.size());
        assertEquals("#/address/street", errors.get(0).pointer());
        assertTrue(problem.getProperties().containsKey("timestamp"));
    }
}
