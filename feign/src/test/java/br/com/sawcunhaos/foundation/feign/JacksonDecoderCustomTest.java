
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

package br.com.sawcunhaos.foundation.feign;

import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * First test ever written for this class (moved from {@code utils} in Story 1.13, never had one).
 * Covers the real branching logic: 404/204 short-circuit, null/empty body, and the
 * mark/reset-before-read trick used to distinguish "empty body" from "has content."
 */
class JacksonDecoderCustomTest {

    private final JacksonDecoderCustom decoder = new JacksonDecoderCustom(new ObjectMapper());

    @Test
    void returnsNullFor404() throws Exception {
        Response response = responseOf(404, "{\"name\":\"x\"}");
        assertNull(decoder.decode(response, String.class));
    }

    @Test
    void returnsNullFor204() throws Exception {
        Response response = responseOf(204, "{\"name\":\"x\"}");
        assertNull(decoder.decode(response, String.class));
    }

    @Test
    void returnsNullForNullBody() throws Exception {
        Response response = Response.builder()
                .status(200)
                .reason("OK")
                .headers(Collections.emptyMap())
                .request(request())
                .build();

        assertNull(decoder.decode(response, String.class));
    }

    @Test
    void returnsNullForEmptyBody() throws Exception {
        Response response = responseOf(200, "");
        assertNull(decoder.decode(response, String.class));
    }

    @Test
    void decodesJsonBody() throws Exception {
        Response response = responseOf(200, "\"hello\"");
        Object decoded = decoder.decode(response, String.class);
        assertEquals("hello", decoded);
    }

    private Response responseOf(int status, String body) {
        return Response.builder()
                .status(status)
                .reason("status " + status)
                .headers(Collections.emptyMap())
                .request(request())
                .body(body, StandardCharsets.UTF_8)
                .build();
    }

    @SuppressWarnings("deprecation")
    private Request request() {
        return Request.create(Request.HttpMethod.GET, "http://example.org", Collections.emptyMap(),
                (byte[]) null, StandardCharsets.UTF_8);
    }
}
