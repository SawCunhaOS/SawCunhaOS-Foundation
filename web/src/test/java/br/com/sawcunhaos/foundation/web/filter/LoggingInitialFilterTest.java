
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

package br.com.sawcunhaos.foundation.web.filter;

import br.com.sawcunhaos.foundation.privacy.SanitizationBodyComponent;
import br.com.sawcunhaos.foundation.privacy.SanitizationHeadersComponent;
import br.com.sawcunhaos.foundation.web.IpAddressExtractor;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LoggingInitialFilter}. They capture the MDC state
 * <em>during</em> the chain (a {@code FilterChain} probe), because the filter's
 * {@code finally} clears the MDC by the time {@code doFilter} returns.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoggingInitialFilterTest {

    @Mock ScosFilterProperties properties;
    @Mock SanitizationHeadersComponent headersComponent;
    @Mock SanitizationBodyComponent bodyComponent;
    @Mock IpAddressExtractor ipExtractor;

    @InjectMocks LoggingInitialFilter filter;

    @AfterEach
    void cleanMdc() {
        MDC.clear();
    }

    /** Captures selected MDC values at the moment the chain executes. */
    private static class MdcProbe implements FilterChain {
        String requestId;
        String requestContentType;
        String requestBody;
        @Override
        public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
            requestId = MDC.get("X-Request-ID");
            requestContentType = MDC.get("Request-Content-Type");
            requestBody = MDC.get("Request-Body");
        }
    }

    @Test
    @DisplayName("reuses the inbound X-Request-ID and echoes it in the response")
    void reusesProvidedRequestId() throws ServletException, IOException {
        when(properties.getURI()).thenReturn("/api");
        when(ipExtractor.extractClientIp(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("X-Request-ID", "my-fixed-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MdcProbe probe = new MdcProbe();

        filter.doFilter(request, response, probe);

        assertEquals("my-fixed-id", probe.requestId);
        assertEquals("my-fixed-id", response.getHeader("X-Request-ID"));
    }

    @Test
    @DisplayName("generates a UUID X-Request-ID when the header is absent")
    void generatesUuidWhenHeaderAbsent() throws ServletException, IOException {
        when(properties.getURI()).thenReturn("/api");
        when(ipExtractor.extractClientIp(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MdcProbe probe = new MdcProbe();

        filter.doFilter(request, new MockHttpServletResponse(), probe);

        assertNotNull(probe.requestId);
        assertDoesNotThrow(() -> UUID.fromString(probe.requestId));
    }

    @Test
    @DisplayName("populates X-Request-ID even for non-/api URIs")
    void populatesRequestIdForNonApiUri() throws ServletException, IOException {
        when(properties.getURI()).thenReturn("/api");
        when(ipExtractor.extractClientIp(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MdcProbe probe = new MdcProbe();

        filter.doFilter(request, new MockHttpServletResponse(), probe);

        assertNotNull(probe.requestId);
    }

    @Test
    @DisplayName("does not leak Request-Content-Type / Request-Body into the chain")
    void doesNotLeakRequestContentTypeAndBody() throws ServletException, IOException {
        when(properties.getURI()).thenReturn("/api");
        when(ipExtractor.extractClientIp(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        request.setContentType("application/json");
        request.setContent("{\"x\":1}".getBytes());
        MdcProbe probe = new MdcProbe();

        filter.doFilter(request, new MockHttpServletResponse(), probe);

        assertNull(probe.requestContentType);
        assertNull(probe.requestBody);
    }

    @Test
    @DisplayName("clears the MDC even when the chain throws, and re-propagates")
    void clearsMdcWhenChainThrows() {
        lenient().when(properties.getURI()).thenReturn("/api");
        when(ipExtractor.extractClientIp(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        FilterChain throwing = (req, res) -> { throw new RuntimeException("boom"); };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> filter.doFilter(request, new MockHttpServletResponse(), throwing));

        assertEquals("boom", ex.getMessage());
        assertTrue(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty());
    }
}
