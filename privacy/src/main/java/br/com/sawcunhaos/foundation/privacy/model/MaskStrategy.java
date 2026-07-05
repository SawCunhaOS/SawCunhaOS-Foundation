
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

package br.com.sawcunhaos.foundation.privacy.model;

import java.util.Locale;

/**
 * Closed set of masking strategies supported by the engine.
 *
 * <p>The set is intentionally <strong>closed</strong>: an unknown strategy in the YAML configuration
 * must fail at load time rather than silently leak data in production. Each strategy documents whether
 * the transformation is reversible.</p>
 *
 * <ul>
 *     <li>{@link #FIXED} — replace the whole value by a constant (the default when only {@code value} is given).</li>
 *     <li>{@link #PARTIAL} — keep a few leading/trailing characters and mask the middle (fail-safe: masks
 *         everything when {@code keepFirst + keepLast >= length}).</li>
 *     <li>{@link #EMAIL} — mask the local part and/or domain of an e-mail address.</li>
 *     <li>{@link #HASH} — pseudonymization via keyed HMAC; stable for correlation, not reversible.</li>
 *     <li>{@link #ENCRYPT} — reversible at-rest encryption (audit trail); only valid for key-based rules.</li>
 *     <li>{@link #REDACT} — remove the field/value from the output entirely.</li>
 * </ul>
 */
public enum MaskStrategy {

    FIXED,
    PARTIAL,
    EMAIL,
    HASH,
    ENCRYPT,
    REDACT;

    /**
     * Resolves a strategy from its YAML token (case-insensitive). Returns {@link #FIXED} for {@code null}
     * or blank, matching the documented default "fixed when only value is given".
     *
     * @param token the raw value read from YAML (may be {@code null})
     * @return the resolved strategy
     * @throws IllegalArgumentException if the token is non-blank but does not match any strategy — this is
     *         the fail-fast behaviour required at load time
     */
    public static MaskStrategy from(final String token) {
        if (token == null || token.isBlank()) {
            return FIXED;
        }
        try {
            return MaskStrategy.valueOf(token.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "Unknown masking strategy '" + token + "'. Valid values: fixed, partial, email, hash, encrypt, redact",
                ex);
        }
    }

    /**
     * Strategies that may be applied to free-text log patterns (operating on the matched group).
     * {@code email}/{@code encrypt}/{@code redact} are key-based only and rejected for {@code log-patterns}.
     *
     * @return {@code true} when this strategy is allowed for {@code log-patterns}
     */
    public boolean isAllowedForLogPatterns() {
        return this == FIXED || this == PARTIAL || this == HASH;
    }
}
