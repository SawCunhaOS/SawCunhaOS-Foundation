
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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration of the source-resolution contract in {@link PrivacyConfigLoader#load(String)}:
 * external path &rarr; classpath ({@code privacy-masking.yml}) &rarr; builtins + WARN.
 */
class PrivacyConfigSourceResolutionTest {

    private static boolean hasBodyKey(final PrivacyConfig config, final String key) {
        return config.getBody().stream().anyMatch(rule -> key.equalsIgnoreCase(rule.getKey()));
    }

    @Test
    void externalPathOverridesClasspath(@TempDir Path dir) throws Exception {
        final Path external = dir.resolve("masking.yml");
        Files.writeString(external, """
            scos:
              privacy:
                masking:
                  body:
                    - key: externalonly
                      strategy: fixed
                      value: "***"
            """);

        final PrivacyConfig config = PrivacyConfigLoader.load(external.toString());

        // The external file wins: its rule is present and the classpath default ("password") is not.
        assertTrue(hasBodyKey(config, "externalonly"));
        assertFalse(hasBodyKey(config, "password"));
    }

    @Test
    void fallsBackToClasspathWhenNoExternalPath() {
        final PrivacyConfig config = PrivacyConfigLoader.load(null);

        // The bundled privacy-masking.yml on the classpath defines these rules.
        assertTrue(hasBodyKey(config, "cpf"));
        assertTrue(hasBodyKey(config, "password"));
    }

    @Test
    void fallsBackToClasspathWhenExternalPathMissing() {
        final PrivacyConfig config = PrivacyConfigLoader.load("/no/such/privacy-masking.yml");

        assertTrue(hasBodyKey(config, "cpf"));
    }
}
