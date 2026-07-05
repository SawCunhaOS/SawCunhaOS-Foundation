
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
import br.com.sawcunhaos.foundation.privacy.crypto.JasyptCryptoKeyProvider;
import br.com.sawcunhaos.foundation.privacy.model.DataMask;
import br.com.sawcunhaos.foundation.privacy.model.MaskStrategy;
import br.com.sawcunhaos.foundation.privacy.specification.DataMaskingValues;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.NotBlank;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Property-based checks for the most important invariant: a masked value never exposes the original PII,
 * masking is idempotent and {@code hash} pseudonymization is stable.
 */
class MaskingInvariantPropertyTest {

    private static MaskingEngine fixedKeyEngine() {
        final DataMaskingValues spi = new DataMaskingValues() {
            @Override
            public Set<DataMask> bodyValue() {
                return Set.of(DataMask.ofKey("pii", MaskStrategy.FIXED, "***"));
            }
        };
        return MaskingEngine.builder().config(PrivacyConfig.empty()).addSpi(spi).build();
    }

    private static MaskingEngine hashKeyEngine() {
        final DataMaskingValues spi = new DataMaskingValues() {
            @Override
            public Set<DataMask> bodyValue() {
                return Set.of(DataMask.builder()
                    .key("uid").strategy(MaskStrategy.HASH).maskChar('*').preserveLength(true).build());
            }
        };
        return MaskingEngine.builder()
            .config(PrivacyConfig.empty())
            .keyProvider(new JasyptCryptoKeyProvider("prop-secret"))
            .addSpi(spi)
            .build();
    }

    @Property
    void fixedMaskNeverExposesOriginal(@ForAll @NotBlank @AlphaChars String value) {
        assertEquals("***", fixedKeyEngine().maskStructured("pii", value));
    }

    @Property
    void maskingIsIdempotent(@ForAll @NotBlank @AlphaChars String value) {
        final MaskingEngine engine = fixedKeyEngine();
        final String once = engine.maskStructured("pii", value);
        final String twice = engine.maskStructured("pii", once);
        assertEquals(once, twice);
    }

    @Property
    void hashIsStable(@ForAll @NotBlank @AlphaChars String a) {
        final MaskingEngine engine = hashKeyEngine();
        assertEquals(engine.maskStructured("uid", a), engine.maskStructured("uid", a));
    }
}
