
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

import br.com.sawcunhaos.foundation.core.enums.ScosExceptionCode;
import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.foundation.core.specification.LocaleService;
import br.com.sawcunhaos.foundation.core.exception.ScosNoRollbackException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Story 2.6: {@code resolveTitle} must not use try/catch as flow control. When
 * {@link LocaleService} fails to resolve the title, the response must fall back
 * to the {@link ScosException}'s own title (from {@code ExceptionCode.getTitle()})
 * instead of a hardcoded English literal that masks the real failure.
 */
@ExtendWith(MockitoExtension.class)
class ExceptionsHandlerResolveTitleTest {

    @Mock LocaleService localeService;
    @InjectMocks ExceptionsHandler handler;

    @Test
    @DisplayName("LocaleService unable to translate the title falls back to ExceptionCode.getTitle(), not the hardcoded 'Business Error' literal")
    void localeServiceFailureFallsBackToExceptionCodeTitle() {
        ScosExceptionCode code = ScosExceptionCode.CPF_INVALID;
        // Simulates what Spring's 4-arg MessageSource.getMessage(code, args, defaultValue, locale)
        // natively does when no bundle entry exists for the code: return the default, untranslated.
        when(localeService.getMessageOrDefault(eq(code.getTitle()), eq(code.getTitle())))
                .thenReturn(code.getTitle());
        when(localeService.getMessage(eq(code.getCode()), any(Object[].class)))
                .thenReturn("detail message");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/persons");
        ScosException exception = new ScosException(code, "cpf");

        ResponseEntity<ProblemDetail> response = handler.handleScosException(exception, request);
        ProblemDetail problem = response.getBody();

        assertNotNull(problem);
        assertNotEquals("Business Error", problem.getTitle());
        assertEquals(code.getTitle(), problem.getTitle());
    }

    @Test
    @DisplayName("handleScosNoRollbackException: same resolveTitle fallback, sibling call site")
    void noRollbackAlsoFallsBackToExceptionCodeTitle() {
        ScosExceptionCode code = ScosExceptionCode.CPF_INVALID;
        when(localeService.getMessageOrDefault(eq(code.getTitle()), eq(code.getTitle())))
                .thenReturn(code.getTitle());
        when(localeService.getMessage(eq(code.getCode())))
                .thenReturn("detail message");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/persons");
        ScosNoRollbackException exception = new ScosNoRollbackException(code);

        ResponseEntity<ProblemDetail> response = handler.handleScosNoRollbackException(exception, request);
        ProblemDetail problem = response.getBody();

        assertNotNull(problem);
        assertNotEquals("Business Error", problem.getTitle());
        assertEquals(code.getTitle(), problem.getTitle());
    }
}
