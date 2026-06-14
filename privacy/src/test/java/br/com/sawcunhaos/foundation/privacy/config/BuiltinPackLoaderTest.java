
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinPackLoaderTest {

    @Test
    void enablesPackByName() {
        final PrivacyConfig config = new PrivacyConfig();
        config.addEnabledBuiltin("br");
        final List<BuiltinPackLoader.BuiltinPattern> patterns = BuiltinPackLoader.resolve(config);
        assertTrue(patterns.stream().anyMatch(p -> "br.cpf".equals(p.mask().key())));
        assertTrue(patterns.stream().anyMatch(p -> "br.cnpj".equals(p.mask().key())));
    }

    @Test
    void disablesSingleItem() {
        final PrivacyConfig config = new PrivacyConfig();
        config.addEnabledBuiltin("br");
        config.addDisabledBuiltin("br.titulo-eleitor");
        final List<BuiltinPackLoader.BuiltinPattern> patterns = BuiltinPackLoader.resolve(config);
        assertFalse(patterns.stream().anyMatch(p -> "br.titulo-eleitor".equals(p.mask().key())));
        assertTrue(patterns.stream().anyMatch(p -> "br.cpf".equals(p.mask().key())));
    }

    @Test
    void cpfPatternCarriesValidator() {
        final PrivacyConfig config = new PrivacyConfig();
        config.addEnabledBuiltin("br");
        final List<BuiltinPackLoader.BuiltinPattern> patterns = BuiltinPackLoader.resolve(config);
        assertTrue(patterns.stream()
            .filter(p -> "br.cpf".equals(p.mask().key()))
            .anyMatch(p -> "cpf".equals(p.validator())));
    }
}
