
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

import br.com.sawcunhaos.foundation.core.enums.ScosExceptionCode;
import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.foundation.core.specification.LocaleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies MDC correlation enrichment of the {@link ProblemDetail}: the
 * {@code requestId} extension mirrors the MDC {@code X-Request-ID} when present
 * and is silently omitted otherwise; {@code timestamp} is always ISO-8601.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExceptionsHandlerMdcTest {

    @Mock LocaleService localeService;
    @InjectMocks ExceptionsHandler handler;

    @AfterEach
    void cleanMdc() {
        MDC.clear();
    }

    private ProblemDetail handle() {
        when(localeService.getMessage(anyString(), any(Object[].class))).thenReturn("erro");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/persons");
        ScosException exception = new ScosException(ScosExceptionCode.CPF_INVALID, "cpf");
        ResponseEntity<ProblemDetail> response = handler.handleScosException(exception, request);
        return response.getBody();
    }

    @Test
    @DisplayName("requestId mirrors MDC X-Request-ID when present")
    void requestIdFromMdc() {
        MDC.put("X-Request-ID", "abc-123");

        ProblemDetail problem = handle();

        assertNotNull(problem);
        assertEquals("abc-123", problem.getProperties().get("requestId"));
    }

    @Test
    @DisplayName("requestId absent when MDC has none; no error")
    void requestIdAbsentWithoutMdc() {
        ProblemDetail problem = handle();

        assertNotNull(problem);
        assertFalse(problem.getProperties().containsKey("requestId"));
    }

    @Test
    @DisplayName("timestamp is always present and ISO-8601 parseable")
    void timestampAlwaysPresent() {
        ProblemDetail problem = handle();

        Object timestamp = problem.getProperties().get("timestamp");
        assertNotNull(timestamp);
        assertDoesNotThrow(() -> Instant.parse(timestamp.toString()));
    }
}
