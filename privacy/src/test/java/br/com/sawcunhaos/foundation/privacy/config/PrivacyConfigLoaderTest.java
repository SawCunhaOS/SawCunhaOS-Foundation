
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

package br.com.sawcunhaos.foundation.privacy.config;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivacyConfigLoaderTest {

    private static InputStream yaml(final String body) {
        return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parsesValidConfig() {
        final PrivacyConfig config = PrivacyConfigLoader.parseAndValidate(yaml("""
            scos:
              privacy:
                masking:
                  body:
                    - key: cpf
                      strategy: partial
                      keep-last: 2
                  audit-encrypt-fields:
                    - cpf
            """));
        assertEquals(1, config.getBody().size());
        assertTrue(config.getAuditEncryptFields().contains("cpf"));
    }

    @Test
    void rejectsUnknownKey() {
        final PrivacyConfigException ex = assertThrows(PrivacyConfigException.class, () ->
            PrivacyConfigLoader.parseAndValidate(yaml("""
                scos:
                  privacy:
                    masking:
                      body:
                        - key: cpf
                          bogus: 1
                """)));
        assertTrue(ex.getMessage().contains("Unknown key"));
    }

    @Test
    void rejectsInvalidStrategy() {
        assertThrows(IllegalArgumentException.class, () ->
            PrivacyConfigLoader.parseAndValidate(yaml("""
                scos:
                  privacy:
                    masking:
                      body:
                        - key: cpf
                          strategy: nope
                """)));
    }

    @Test
    void rejectsNegativeKeep() {
        assertThrows(PrivacyConfigException.class, () ->
            PrivacyConfigLoader.parseAndValidate(yaml("""
                scos:
                  privacy:
                    masking:
                      body:
                        - key: cpf
                          strategy: partial
                          keep-first: -1
                """)));
    }

    @Test
    void rejectsMultiCharMaskChar() {
        assertThrows(PrivacyConfigException.class, () ->
            PrivacyConfigLoader.parseAndValidate(yaml("""
                scos:
                  privacy:
                    masking:
                      body:
                        - key: cpf
                          strategy: partial
                          mask-char: "##"
                """)));
    }

    @Test
    void rejectsForbiddenStrategyOnLogPatterns() {
        assertThrows(PrivacyConfigException.class, () ->
            PrivacyConfigLoader.parseAndValidate(yaml("""
                scos:
                  privacy:
                    masking:
                      log-patterns:
                        - regex: '\\\\d+'
                          strategy: redact
                """)));
    }

    @Test
    void missingSourceReturnsEmptyConfig() {
        // A path that does not exist and no classpath override in this stream-less call.
        final PrivacyConfig config = PrivacyConfigLoader.parseAndValidate(yaml("not: a masking doc"));
        assertEquals(0, config.getBody().size());
    }
}
