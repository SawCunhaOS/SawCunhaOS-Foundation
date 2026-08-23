
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

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * First test ever written for this class (moved from {@code utils} in Story 1.13, never had one).
 */
class JacksonEncoderCustomTest {

    private final JacksonEncoderCustom encoder = new JacksonEncoderCustom(new ObjectMapper());

    @Test
    void encodesObjectAsJsonBodyOnTemplate() {
        RequestTemplate template = new RequestTemplate();

        encoder.encode("hello", String.class, template);

        assertEquals("\"hello\"", new String(template.body(), StandardCharsets.UTF_8));
    }
}
