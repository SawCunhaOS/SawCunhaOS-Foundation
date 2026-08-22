
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
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonMaskerTest {

    private JsonMasker maskerFromBasic() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("test-masking-basic.yml")) {
            assertNotNull(in, "fixture must be on classpath");
            final PrivacyConfig config = PrivacyConfigLoader.parseAndValidate(in);
            final MaskingEngine engine = MaskingEngine.builder()
                    .config(config)
                    .keyProvider(new JasyptCryptoKeyProvider("unit-secret"))
                    .build();
            return new JsonMasker(engine);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void masksStringFieldInObject() {
        final JsonMasker masker = maskerFromBasic();
        final String masked = masker.mask("{\"email\":\"ana@x.com\"}");
        assertEquals("{\"email\":\"a***@***.com\"}", masked);
    }

    @Test
    void integerFieldWithMatchingRuleIsMaskedAsText() {
        // cpf: partial, keep-first 0, keep-last 2 — value is a JSON integer, Jackson parses it as IntNode.
        final JsonMasker masker = maskerFromBasic();
        final String masked = masker.mask("{\"cpf\":12345678901}");
        assertEquals("{\"cpf\":\"*********01\"}", masked);
    }

    @Test
    void longFieldWithNoRuleIsLeftUnchanged() {
        // No rule for this key: value must survive untouched, including its original numeric shape
        // (Jackson would parse this as a LongNode; Gson used a lazily-parsed Double/BigDecimal-like type
        // for the same input — the traversal must not depend on the concrete numeric subtype).
        final JsonMasker masker = maskerFromBasic();
        final String masked = masker.mask("{\"unknownField\":123456789012345}");
        assertEquals("{\"unknownField\":123456789012345}", masked);
    }

    @Test
    void bigDecimalFieldWithNoRuleIsLeftUnchanged() {
        final JsonMasker masker = maskerFromBasic();
        final String masked = masker.mask("{\"unknownField\":1234.56789}");
        assertEquals("{\"unknownField\":1234.56789}", masked);
    }

    @Test
    void redactStrategySetsNullKeepingKey() {
        final JsonMasker masker = maskerFromBasic();
        final String masked = masker.mask("{\"internalsecret\":42}");
        assertEquals("{\"internalsecret\":null}", masked);
    }

    @Test
    void nestedObjectsAndArraysAreWalked() {
        final JsonMasker masker = maskerFromBasic();
        final String masked = masker.mask("{\"user\":{\"email\":\"ana@x.com\"},\"items\":[{\"cpf\":12345678901}]}");
        assertTrue(masked.contains("\"a***@***.com\""));
        assertTrue(masked.contains("\"*********01\""));
    }

    @Test
    void nonJsonInputIsReturnedUnchanged() {
        final JsonMasker masker = maskerFromBasic();
        assertEquals("not json at all", masker.mask("not json at all"));
    }

    @Test
    void nullAndEmptyInputAreReturnedUnchanged() {
        final JsonMasker masker = maskerFromBasic();
        assertEquals(null, masker.mask(null));
        assertEquals("", masker.mask(""));
    }
}
