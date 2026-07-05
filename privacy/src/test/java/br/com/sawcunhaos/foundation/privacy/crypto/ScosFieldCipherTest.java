
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

package br.com.sawcunhaos.foundation.privacy.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScosFieldCipherTest {

    private final ScosFieldCipher cipher = new ScosFieldCipher(new JasyptCryptoKeyProvider("round-trip-secret"));

    @Test
    void roundTripRecoversPlaintext() {
        final String token = cipher.encrypt("12345678901");
        assertTrue(token.startsWith("enc:v"));
        assertEquals("12345678901", cipher.decrypt(token));
    }

    @Test
    void ciphertextCarriesKeyId() {
        final String token = cipher.encrypt("ana@x.com");
        assertTrue(token.startsWith("enc:vv1:"), "token must embed the key id: " + token);
    }

    @Test
    void distinctIvsProduceDistinctCiphertexts() {
        assertNotEquals(cipher.encrypt("same"), cipher.encrypt("same"));
    }

    @Test
    void decryptIgnoresNonTokens() {
        assertEquals("plain", cipher.decrypt("plain"));
    }
}
