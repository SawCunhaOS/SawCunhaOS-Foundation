
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

package br.com.sawcunhaos.foundation.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashing helpers. Read the contract of each method before using it for personal data.
 *
 * <p><strong>Two very different guarantees live here:</strong></p>
 * <ul>
 *   <li>{@link #createHash(String)} is a plain, unkeyed SHA-256 digest. It is a
 *       <em>checksum / idempotency</em> primitive, <strong>not</strong> a privacy mechanism: for low-entropy
 *       inputs (CPF, e-mail, phone) it is trivially reversible by rainbow tables or brute force, so it does
 *       <em>not</em> pseudonymize personal data under LGPD Art. 13 / GDPR Art. 4(5).</li>
 *   <li>{@link #pseudonymize(String, String)} is a keyed HMAC-SHA256. Because the secret key is required to
 *       reproduce the token, it is a sound pseudonymization primitive for personal data.</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class HashUtils {

    private static final String ALGORITHM_HASH = "SHA-256";
    private static final String ALGORITHM_HMAC = "HmacSHA256";

    /**
     * Computes an unkeyed SHA-256 digest of the value, hex-encoded.
     *
     * <p><strong>Not a privacy mechanism.</strong> Use only for checksums, idempotency keys, cache keys and
     * similar non-sensitive fingerprints. To pseudonymize personal data, use {@link #pseudonymize(String, String)}.</p>
     *
     * @param value the input to digest
     * @return the lowercase hex SHA-256, or {@code null} if the algorithm is unavailable
     */
    public static String createHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM_HASH);
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return toHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error create Hash", e);
            return null;
        }
    }

    /**
     * Pseudonymizes a value with HMAC-SHA256 under a secret key, producing a stable, hex-encoded token.
     *
     * <p>The same {@code value} + {@code secret} always yields the same token (so it can be used to correlate
     * records), but the token cannot be reversed to the original without the secret. This is the appropriate
     * primitive for pseudonymizing personal data under LGPD Art. 13 / GDPR Art. 4(5); keep the secret in a
     * vault/KMS and rotate it per the data-retention policy.</p>
     *
     * @param value the personal data to pseudonymize (must not be {@code null})
     * @param secret the secret key (must not be {@code null} or blank)
     * @return the lowercase hex HMAC-SHA256 token
     * @throws IllegalArgumentException if {@code value} or {@code secret} is missing
     * @throws IllegalStateException if HMAC computation fails
     */
    public static String pseudonymize(final String value, final String secret) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("secret must not be null or blank");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM_HMAC);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM_HMAC));
            return toHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC pseudonym", e);
        }
    }

    private static String toHex(final byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
