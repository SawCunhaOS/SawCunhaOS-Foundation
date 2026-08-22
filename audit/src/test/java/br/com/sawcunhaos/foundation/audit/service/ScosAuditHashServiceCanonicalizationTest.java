
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

package br.com.sawcunhaos.foundation.audit.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Determinism invariant for the JSON canonicalization the hash-chain hashes over (AC7): the same
 * input must always canonicalize to the same string, across repeated calls and regardless of key
 * order. This guards the migration from Gson to Jackson going forward, not just at the cutover.
 */
class ScosAuditHashServiceCanonicalizationTest {

    @Test
    void canonicalizationIsDeterministicAcrossRepeatedCalls() {
        String json = "{\"b\":2,\"a\":1,\"nested\":{\"z\":true,\"y\":3.5}}";
        String first = ScosAuditHashService.canonicalizeJson(json);
        for (int i = 0; i < 50; i++) {
            assertEquals(first, ScosAuditHashService.canonicalizeJson(json));
        }
    }

    @Test
    void keyOrderDoesNotAffectCanonicalOutput() {
        String a = "{\"b\":2,\"a\":1}";
        String b = "{\"a\":1,\"b\":2}";
        assertEquals(ScosAuditHashService.canonicalizeJson(a), ScosAuditHashService.canonicalizeJson(b));
    }

    @Test
    void numberFormattingIsStableAcrossRuns() {
        String json = "{\"amount\":1234.5678,\"count\":42}";
        String first = ScosAuditHashService.canonicalizeJson(json);
        assertEquals(first, ScosAuditHashService.canonicalizeJson(json));
        assertTrue(first.contains("1234.5678"));
        assertTrue(first.contains("42"));
    }

    @Test
    void nullOrBlankInputCanonicalizesToEmptyString() {
        assertEquals("", ScosAuditHashService.canonicalizeJson(null));
        assertEquals("", ScosAuditHashService.canonicalizeJson("  "));
    }

    @Test
    void invalidJsonFallsBackToOriginalString() {
        String invalid = "not json";
        assertEquals(invalid, ScosAuditHashService.canonicalizeJson(invalid));
    }
}
