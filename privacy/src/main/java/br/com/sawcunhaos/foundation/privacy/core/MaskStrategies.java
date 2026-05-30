
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

import br.com.sawcunhaos.foundation.privacy.crypto.ScosCryptoKeyProvider;
import br.com.sawcunhaos.foundation.privacy.crypto.ScosFieldCipher;
import br.com.sawcunhaos.foundation.privacy.model.DataMask;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Pure functions that apply a {@link br.com.sawcunhaos.foundation.privacy.model.MaskStrategy} to a value.
 *
 * <p>All methods are stateless; the engine passes the matched value and the rule. The class centralizes
 * the fail-safe rules (e.g. {@code partial} masking everything when the kept window covers the value) so
 * the behaviour is identical across HTTP, log and audit surfaces.</p>
 */
final class MaskStrategies {

    private final ScosCryptoKeyProvider keyProvider;
    private final ScosFieldCipher fieldCipher;
    private final boolean strict;

    MaskStrategies(final ScosCryptoKeyProvider keyProvider, final ScosFieldCipher fieldCipher, final boolean strict) {
        this.keyProvider = keyProvider;
        this.fieldCipher = fieldCipher;
        this.strict = strict;
    }

    /**
     * Applies the rule's strategy to a single value.
     *
     * @param rule the masking rule
     * @param value the original value
     * @return the masked value; {@code null} for {@code redact}
     */
    String apply(final DataMask rule, final String value) {
        if (value == null) {
            return null;
        }
        return switch (rule.strategy()) {
            case FIXED -> fixed(rule);
            case PARTIAL -> partial(rule, value);
            case EMAIL -> email(rule, value);
            case HASH -> hash(value);
            case ENCRYPT -> encrypt(rule, value);
            case REDACT -> null;
        };
    }

    private String fixed(final DataMask rule) {
        return rule.newValue() != null ? rule.newValue() : DataMask.DEFAULT_VALUE;
    }

    /**
     * Keeps {@code keepFirst} leading and {@code keepLast} trailing characters, masking the middle.
     * Fail-safe: when the kept window is greater than or equal to the length, masks everything so a short
     * value is never partially revealed.
     */
    private String partial(final DataMask rule, final String value) {
        final int len = value.length();
        final int first = Math.max(0, rule.keepFirst());
        final int last = Math.max(0, rule.keepLast());
        final char mc = rule.maskChar() == 0 ? DataMask.DEFAULT_MASK_CHAR : rule.maskChar();

        if (first + last >= len) {
            return String.valueOf(mc).repeat(len);
        }
        final int middle = len - first - last;
        final int fill = rule.preserveLength() ? middle : Math.min(middle, 3);
        final StringBuilder sb = new StringBuilder(first + fill + last);
        sb.append(value, 0, first);
        sb.append(String.valueOf(mc).repeat(fill));
        sb.append(value, len - last, len);
        return sb.toString();
    }

    /**
     * Masks the local part and/or the domain of an e-mail. Non-e-mail values fall back to {@code fixed}.
     */
    private String email(final DataMask rule, final String value) {
        final int at = value.indexOf('@');
        if (at <= 0 || at == value.length() - 1) {
            return fixed(rule);
        }
        final boolean maskLocal = true; // defaults documented as true; engine fills rule flags
        final String local = value.substring(0, at);
        final String domain = value.substring(at + 1);
        final String maskedLocal = maskLocal
            ? local.charAt(0) + "***"
            : local;
        final int dot = domain.lastIndexOf('.');
        final String maskedDomain = dot > 0 ? "***" + domain.substring(dot) : "***";
        return maskedLocal + "@" + maskedDomain;
    }

    /**
     * Keyed HMAC-SHA256 pseudonymization: stable for correlation, not reversible. Distinct from a bare
     * SHA-256 (which would be reversible by rainbow table) — this satisfies LGPD Art. 13 / GDPR Art. 4(5).
     */
    private String hash(final String value) {
        if (keyProvider == null) {
            return degrade("hash");
        }
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyProvider.hmacKey(), "HmacSHA256"));
            final byte[] out = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return "h:" + HexFormat.of().formatHex(out, 0, 8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to apply hash strategy", ex);
        }
    }

    private String encrypt(final DataMask rule, final String value) {
        if (fieldCipher == null) {
            return degrade("encrypt");
        }
        return fieldCipher.encrypt(value);
    }

    /**
     * In strict mode a missing crypto provider is a startup error; otherwise the engine degrades the
     * affected value to a fixed mask so nothing is ever emitted in clear.
     */
    private String degrade(final String strategy) {
        if (strict) {
            throw new IllegalStateException("Strategy '" + strategy
                + "' requires a ScosCryptoKeyProvider but none is configured (strict mode)");
        }
        return DataMask.DEFAULT_VALUE;
    }
}
