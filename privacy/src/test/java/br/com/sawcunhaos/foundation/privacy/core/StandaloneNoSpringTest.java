
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

package br.com.sawcunhaos.foundation.privacy.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the engine is usable outside the SCOS layer: built straight from a YAML file with no Spring
 * context in scope.
 */
class StandaloneNoSpringTest {

    @Test
    void buildsFromYamlWithoutSpring() throws Exception {
        final Path yaml = Files.createTempFile("privacy-masking", ".yml");
        Files.writeString(yaml, """
            scos:
              privacy:
                masking:
                  builtins:
                    enabled: [br]
                  body:
                    - key: cpf
                      strategy: partial
                      keep-last: 2
            """);
        try {
            final MaskingEngine engine = MaskingEngine.fromYaml(yaml);
            assertTrue(engine.maskStructured("cpf", "12345678901").endsWith("01"));
            assertTrue(!engine.maskText("cpf 529.982.247-25").contains("529.982.247-25"));
        } finally {
            Files.deleteIfExists(yaml);
        }
    }
}
