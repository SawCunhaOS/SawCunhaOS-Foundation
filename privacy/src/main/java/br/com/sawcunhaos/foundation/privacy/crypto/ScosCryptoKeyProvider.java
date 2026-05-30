
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

/**
 * SPI for supplying cryptographic keys to the field cipher and the keyed-hash (HMAC) pseudonymization.
 *
 * <p>The default implementation reads an externalized secret (see {@code JasyptCryptoKeyProvider}). An
 * application may plug Vault/KMS by providing its own bean. Keys are <strong>versioned</strong>: each
 * encrypted record stores the {@link #currentKeyId()} used, and decryption resolves the key by that id.
 * Rotation therefore affects only new records — history is never re-encrypted.</p>
 */
public interface ScosCryptoKeyProvider {

    /**
     * @return the identifier of the key currently used for new encryptions (e.g. {@code "v1"})
     */
    String currentKeyId();

    /**
     * Resolves the raw key material for a given key id.
     *
     * @param keyId the version id stored alongside the ciphertext
     * @return the key bytes
     * @throws IllegalArgumentException when the key id is unknown
     */
    byte[] keyFor(String keyId);

    /**
     * @return the secret used for HMAC-based pseudonymization (stable correlation hashing)
     */
    byte[] hmacKey();
}
