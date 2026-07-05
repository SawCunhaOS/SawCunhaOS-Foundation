
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

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Reversible, selective field cipher for at-rest protection of audit-trail PII.
 *
 * <p>Ciphertext is self-describing: {@code enc:v<keyId>:<base64(iv|ciphertext|tag)>}. The key id is
 * stored inside the token so a value can always be decrypted by resolving its key through
 * {@link ScosCryptoKeyProvider}, even after rotation (history is not re-encrypted).</p>
 *
 * <p>Uses AES-256/GCM with a random 12-byte IV per value. The instance is thread-safe: it holds no
 * mutable state and creates a fresh {@link Cipher} per call.</p>
 */
public class ScosFieldCipher {

    /** Prefix that marks an encrypted value and carries its key id. */
    public static final String PREFIX = "enc:";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final ScosCryptoKeyProvider keyProvider;
    private final SecureRandom random = new SecureRandom();

    public ScosFieldCipher(final ScosCryptoKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    /**
     * Encrypts a plaintext value, producing a self-describing token.
     *
     * @param plaintext the value to protect (returned unchanged when {@code null})
     * @return {@code enc:v<keyId>:<base64...>} or {@code null} when input is {@code null}
     */
    public String encrypt(final String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            final String keyId = keyProvider.currentKeyId();
            final byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyProvider.keyFor(keyId), "AES"),
                new GCMParameterSpec(TAG_BITS, iv));
            final byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            final byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return PREFIX + "v" + keyId + ":" + Base64.getEncoder().encodeToString(out);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt field value", ex);
        }
    }

    /**
     * Decrypts a token produced by {@link #encrypt(String)}. Values without the {@link #PREFIX} are
     * returned unchanged, so the method is safe to apply over mixed (already-plaintext) data.
     *
     * @param token the stored value
     * @return the recovered plaintext, or the input unchanged when it is not an encrypted token
     */
    public String decrypt(final String token) {
        if (token == null || !token.startsWith(PREFIX)) {
            return token;
        }
        try {
            final int firstColon = token.indexOf(':');
            final int secondColon = token.indexOf(':', firstColon + 1);
            final String keyId = token.substring(firstColon + 2, secondColon); // skip the 'v' marker
            final byte[] all = Base64.getDecoder().decode(token.substring(secondColon + 1));

            final byte[] iv = new byte[IV_LENGTH];
            final byte[] ct = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            System.arraycopy(all, IV_LENGTH, ct, 0, ct.length);

            final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyProvider.keyFor(keyId), "AES"),
                new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt field value", ex);
        }
    }

    /**
     * @param value any stored value
     * @return {@code true} when the value is an encrypted token
     */
    public boolean isEncrypted(final String value) {
        return value != null && value.startsWith(PREFIX);
    }
}
