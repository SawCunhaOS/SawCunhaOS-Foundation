
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

import br.com.sawcunhaos.foundation.utils.configuration.rest.filter.properties.ScosFilterProperties;
import br.com.sawcunhaos.foundation.privacy.SanitizationBodyComponent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LoggingFinalFilter}: response MDC fields are populated
 * for the configured URI prefix and skipped otherwise. The filter no longer
 * clears the MDC (ownership moved to {@link LoggingInitialFilter}).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoggingFinalFilterTest {

    @Mock ScosFilterProperties properties;
    @Mock SanitizationBodyComponent bodyComponent;

    @InjectMocks LoggingFinalFilter filter;

    @AfterEach
    void cleanMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("populates Response-Code in MDC during the final log for /api")
    void populatesResponseCodeForApi() throws ServletException, IOException {
        when(properties.getURI()).thenReturn("/api");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        FilterChain chain = (req, res) -> { /* no-op */ };

        filter.doFilter(request, response, chain);

        assertEquals("200", MDC.get("Response-Code"));
    }

    @Test
    @DisplayName("does not populate response MDC fields for non-/api URIs")
    void skipsResponseMdcForNonApi() throws ServletException, IOException {
        when(properties.getURI()).thenReturn("/api");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        FilterChain chain = (req, res) -> { /* no-op */ };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNull(MDC.get("Response-Code"));
    }
}
