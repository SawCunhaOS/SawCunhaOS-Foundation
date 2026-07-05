
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Default {@link ScosCryptoKeyProvider} backed by an externalized secret.
 *
 * <p>This is the out-of-the-box implementation so the module works without extra infrastructure. The
 * secret is expected to come from outside the artifact (env/property), never hard-coded. For stronger
 * guarantees an application should replace this bean with a Vault/KMS-backed provider.</p>
 *
 * <p>The key material is derived from the secret with SHA-256 to obtain a fixed 32-byte (AES-256) key.
 * A single key id ({@code v1}) is exposed here; rotation is achieved by deploying a provider that knows
 * multiple ids.</p>
 */
public final class JasyptCryptoKeyProvider implements ScosCryptoKeyProvider {

    private static final String DEFAULT_KEY_ID = "v1";

    private final String keyId;
    private final byte[] key;
    private final byte[] hmacKey;

    /**
     * @param secret the externalized secret (must not be {@code null}/blank)
     */
    public JasyptCryptoKeyProvider(final String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Privacy crypto secret must not be blank");
        }
        this.keyId = DEFAULT_KEY_ID;
        this.key = sha256(secret.getBytes(StandardCharsets.UTF_8));
        // Separate HMAC key derived from the same secret with a distinct domain separator,
        // so the encryption key and the pseudonymization key are not identical.
        this.hmacKey = sha256(("hmac:" + secret).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String currentKeyId() {
        return keyId;
    }

    @Override
    public byte[] keyFor(final String requestedKeyId) {
        if (!keyId.equals(requestedKeyId)) {
            throw new IllegalArgumentException("Unknown key id: " + requestedKeyId);
        }
        return key.clone();
    }

    @Override
    public byte[] hmacKey() {
        return hmacKey.clone();
    }

    private static byte[] sha256(final byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
