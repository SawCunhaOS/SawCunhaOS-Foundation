
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

package br.com.sawcunhaos.foundation.jdempotent.core.generator;

import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.IdempotentTestPayload;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultKeyGeneratorTest {

    private final DefaultKeyGenerator keyGenerator = new DefaultKeyGenerator();

    /**
     * Regression test for the unpadded-hex bug: {@code Integer.toHexString(0xFF & b)} drops the
     * leading zero of any byte below 0x10, so two different digests can render as the exact same
     * hex string. Bytes {0x01, 0x23} and {0x12, 0x03} are such a pair:
     * old formatting -> "1" + "23" = "123" for both, a real collision.
     * {@link java.util.HexFormat#formatHex} zero-pads every byte, so they now render as the
     * distinct, unambiguous strings "0123" and "1203".
     */
    @Test
    void should_produce_distinct_hex_for_digests_that_collide_under_unpadded_formatting() throws NoSuchAlgorithmException {
        //Given
        byte[] digestA = {0x01, 0x23};
        byte[] digestB = {0x12, 0x03};
        assertEquals(legacyUnpaddedHex(digestA), legacyUnpaddedHex(digestB),
                "sanity check: these two digests must collide under the old Integer.toHexString formatting");

        MessageDigest messageDigestA = mock(MessageDigest.class);
        when(messageDigestA.digest()).thenReturn(digestA);
        MessageDigest messageDigestB = mock(MessageDigest.class);
        when(messageDigestB.digest()).thenReturn(digestB);

        IdempotentRequestWrapper requestObject = new IdempotentRequestWrapper(new IdempotentTestPayload("payload"));

        //When
        IdempotencyKey keyA = keyGenerator.generateIdempotentKey(requestObject, "listener", new StringBuilder(), messageDigestA);
        IdempotencyKey keyB = keyGenerator.generateIdempotentKey(requestObject, "listener", new StringBuilder(), messageDigestB);

        //Then
        assertNotEquals(keyA.getKeyValue(), keyB.getKeyValue());
        // Suffix-only check: this test uses the no-arg constructor (no namespace), which never
        // prepends anything before "listener-" (Story 3.10 removed the old APP_NAME env var read).
        assertTrue(keyA.getKeyValue().endsWith("listener-0123"));
        assertTrue(keyB.getKeyValue().endsWith("listener-1203"));
    }

    @Test
    void should_produce_full_length_sha256_hex_for_distinct_payloads() throws NoSuchAlgorithmException {
        //Given
        IdempotentRequestWrapper requestOne = new IdempotentRequestWrapper(new IdempotentTestPayload("payload-one"));
        IdempotentRequestWrapper requestTwo = new IdempotentRequestWrapper(new IdempotentTestPayload("payload-two"));

        //When
        IdempotencyKey keyOne = keyGenerator.generateIdempotentKey(requestOne, "listener", new StringBuilder(),
                MessageDigest.getInstance("SHA-256"));
        IdempotencyKey keyTwo = keyGenerator.generateIdempotentKey(requestTwo, "listener", new StringBuilder(),
                MessageDigest.getInstance("SHA-256"));

        //Then
        assertNotEquals(keyOne.getKeyValue(), keyTwo.getKeyValue());
        // Minimum length check (not exact): "listener-" prefix (9 chars) + 64 hex chars for a full
        // SHA-256 digest (this test's no-arg constructor has no namespace, so there is no extra prefix).
        assertTrue(keyOne.getKeyValue().length() >= 9 + 64);
        assertTrue(keyTwo.getKeyValue().length() >= 9 + 64);
    }

    private static String legacyUnpaddedHex(byte[] digest) {
        StringBuilder builder = new StringBuilder();
        for (byte b : digest) {
            builder.append(Integer.toHexString(0xFF & b));
        }
        return builder.toString();
    }
}
