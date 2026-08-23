
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

import br.com.sawcunhaos.foundation.privacy.DataMaskingService;
import br.com.sawcunhaos.foundation.privacy.SanitizationBodyComponent;
import br.com.sawcunhaos.foundation.privacy.SanitizationHeadersComponent;
import br.com.sawcunhaos.foundation.privacy.core.MaskingEngine;
import br.com.sawcunhaos.foundation.web.IpAddressExtractor;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP end-to-end check that PII in the request body and headers is masked in the filter's log line.
 *
 * <p>Runs standalone (no full Spring context): a real {@link MaskingEngine} from inline YAML backs the
 * deprecated sanitization facades, the two logging filters are wired by hand and registered with a
 * standalone {@link MockMvc}, and a {@link ListAppender} captures the {@code Initial API Call} event to
 * inspect its MDC snapshot.</p>
 */
class LoggingFilterMaskingE2ETest {

    private static final String YAML = """
        scos:
          privacy:
            masking:
              headers:
                - key: authorization
                  strategy: fixed
                  value: "***"
              body:
                - key: cpf
                  strategy: fixed
                  value: "***"
        """;

    private MockMvc mvc;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        final MaskingEngine engine = MaskingEngine.fromYaml(
            new ByteArrayInputStream(YAML.getBytes(StandardCharsets.UTF_8)));
        final DataMaskingService privacyService = new DataMaskingService(engine);
        final SanitizationHeadersComponent headers = new SanitizationHeadersComponent(privacyService);
        final SanitizationBodyComponent body = new SanitizationBodyComponent(privacyService);

        final ScosFilterProperties props = new ScosFilterProperties();
        props.setContextPath("/");
        props.setShowRequestBody(true);
        props.setShowRequestHeaders(true);
        props.setShowResponseBody(true);

        final LoggingInitialFilter initial =
            new LoggingInitialFilter(props, headers, body, new IpAddressExtractor());
        final LoggingFinalFilter last = new LoggingFinalFilter(props, body);

        mvc = MockMvcBuilders.standaloneSetup(new EchoController())
            .addFilters(initial, last)
            .build();

        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger filterLogger = context.getLogger(LoggingInitialFilter.class);
        appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        filterLogger.addAppender(appender);
    }

    @Test
    void masksBodyAndHeaderInTheInitialLogLine() throws Exception {
        mvc.perform(post("/api/echo")
                .header("Authorization", "Bearer super-secret-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cpf\":\"529.982.247-25\",\"name\":\"ada\"}"))
            .andExpect(status().isOk());

        final ILoggingEvent initialCall = appender.list.stream()
            .filter(e -> "Initial API Call".equals(e.getMessage()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Initial API Call log not captured"));

        final Map<String, String> mdc = initialCall.getMDCPropertyMap();

        // Body: the cpf value must not leak; the non-sensitive field is preserved.
        assertFalse(mdc.get("Request-Body").contains("529.982.247-25"), "cpf não deve vazar no log");
        assertTrue(mdc.get("Request-Body").contains("ada"), "campo não sensível deve ser preservado");
        // Header: the bearer token must not leak.
        assertFalse(mdc.get("Headers").contains("super-secret-token"), "token não deve vazar no log");
        assertTrue(mdc.get("Headers").contains("***"), "header deve sair mascarado");
    }

    @RestController
    static class EchoController {
        @PostMapping("/api/echo")
        Map<String, Object> echo(@RequestBody Map<String, Object> payload) {
            return payload;
        }
    }
}
