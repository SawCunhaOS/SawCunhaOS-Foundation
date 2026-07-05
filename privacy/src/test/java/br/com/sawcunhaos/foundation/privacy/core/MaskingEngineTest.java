
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

import br.com.sawcunhaos.foundation.privacy.config.PrivacyConfig;
import br.com.sawcunhaos.foundation.privacy.config.PrivacyConfigLoader;
import br.com.sawcunhaos.foundation.privacy.crypto.JasyptCryptoKeyProvider;
import br.com.sawcunhaos.foundation.privacy.model.DataMask;
import br.com.sawcunhaos.foundation.privacy.model.MaskStrategy;
import br.com.sawcunhaos.foundation.privacy.specification.DataMaskingValues;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskingEngineTest {

    private MaskingEngine engineFromBasic() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("test-masking-basic.yml")) {
            assertNotNull(in, "fixture must be on classpath");
            final PrivacyConfig config = PrivacyConfigLoader.parseAndValidate(in);
            return MaskingEngine.builder().config(config).keyProvider(new JasyptCryptoKeyProvider("unit-secret")).build();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void maskStructuredAppliesPartialKeepingTail() {
        final MaskingEngine engine = engineFromBasic();
        assertEquals("*********01", engine.maskStructured("cpf", "12345678901"));
    }

    @Test
    void maskStructuredReturnsOriginalWhenNoRule() {
        final MaskingEngine engine = engineFromBasic();
        assertEquals("keepme", engine.maskStructured("unknownField", "keepme"));
    }

    @Test
    void emailStrategyMasksLocalAndDomain() {
        final MaskingEngine engine = engineFromBasic();
        assertEquals("a***@***.com", engine.maskStructured("email", "ana@x.com"));
    }

    @Test
    void redactStrategyReturnsNull() {
        final MaskingEngine engine = engineFromBasic();
        assertEquals(null, engine.maskStructured("internalsecret", "value"));
    }

    @Test
    void maskTextFastPathReturnsSameReferenceWhenNoTrigger() {
        // No builtins, no literals -> no trigger -> same reference.
        final MaskingEngine engine = MaskingEngine.builder().config(PrivacyConfig.empty()).build();
        final String line = "plain log line without pii";
        assertSame(line, engine.maskText(line));
    }

    @Test
    void maskTextMasksLiteralNeedle() {
        final MaskingEngine engine = engineFromBasic();
        final String masked = engine.maskText("contains topsecret here");
        assertTrue(masked.contains("***"));
        assertTrue(!masked.contains("topsecret"));
    }

    @Test
    void maskTextMasksBuiltinCpfWithValidDv() {
        final MaskingEngine engine = engineFromBasic();
        // 529.982.247-25 is a valid CPF.
        final String masked = engine.maskText("cliente cpf 529.982.247-25 cadastrado");
        assertTrue(!masked.contains("529.982.247-25"), "valid CPF must be masked");
    }

    @Test
    void maskTextLeavesInvalidCpfUntouched() {
        final MaskingEngine engine = engineFromBasic();
        // 111.111.111-11 matches the regex but fails the DV check.
        final String input = "ruido 111.111.111-11 fim";
        assertEquals(input, engine.maskText(input));
    }

    @Test
    void spiOverridesYamlByKey() {
        final PrivacyConfig empty = PrivacyConfig.empty();
        final DataMaskingValues spi = new DataMaskingValues() {
            @Override
            public Set<DataMask> bodyValue() {
                return Set.of(DataMask.ofKey("apikey", MaskStrategy.FIXED, "<hidden>"));
            }
            @Override
            public Set<String> auditEncryptFields() {
                return Set.of("apikey");
            }
        };
        final MaskingEngine engine = MaskingEngine.builder().config(empty).addSpi(spi).build();
        assertEquals("<hidden>", engine.maskStructured("apikey", "abc123"));
        assertTrue(engine.auditEncryptFields().contains("apikey"));
    }

    @Test
    void capTruncatesOversizedMessage() {
        final MaskingEngine engine = MaskingEngine.builder()
            .config(PrivacyConfig.empty())
            .maxPayloadKb(1)
            .build();
        final String big = "1".repeat(2048); // 2KB, has digit trigger
        final String masked = engine.maskText(big);
        assertTrue(masked.endsWith("...[truncated]"));
    }
}
