
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

package br.com.sawcunhaos.foundation.jdempotent.core.model;

import lombok.Getter;

/**
 * Encodes/decodes the business exception cached under
 * {@code IdempotentFailurePolicy#KEEP_FAILED} (Story 3.8) as a plain
 * {@code String}, not a nested POJO.
 *
 * <p>{@code IdempotentResponseWrapper#response} is declared as {@code Object}.
 * {@code PolymorphicRedisSerializer} only records the concrete type at the
 * root of what is handed to Jackson ({@code IdempotentRequestResponseWrapper}
 * itself) — any custom POJO nested under an {@code Object}-typed field comes
 * back from a real Redis round trip as a generic {@code LinkedHashMap}, not
 * its original type (a pre-existing gap in the module, not specific to this
 * class). A plain {@code String}, by contrast, always round-trips correctly.
 * A fixed, effectively-impossible-to-collide marker distinguishes an encoded
 * failure from a business method that legitimately returns a plain
 * {@code String}.</p>
 */
public final class CachedBusinessFailure {

    private static final String MARKER = "IDEMPOTENT_KEEP_FAILED::";
    private static final String SEPARATOR = "::";

    @Getter
    private final String exceptionClassName;
    @Getter
    private final String exceptionMessage;

    private CachedBusinessFailure(String exceptionClassName, String exceptionMessage) {
        this.exceptionClassName = exceptionClassName;
        this.exceptionMessage = exceptionMessage;
    }

    public static CachedBusinessFailure of(Throwable exception) {
        return new CachedBusinessFailure(exception.getClass().getName(), exception.getMessage());
    }

    /**
     * Encodes this failure as a {@code String}, safe to store as an
     * {@code IdempotentResponseWrapper#response} value.
     */
    public String encode() {
        return MARKER + exceptionClassName + SEPARATOR + (exceptionMessage == null ? "" : exceptionMessage);
    }

    /**
     * Decodes {@code cachedResponse} if it is a failure encoded by
     * {@link #encode()}, or returns {@code null} otherwise (including when
     * it isn't even a {@code String} — a normal cached success response).
     */
    public static CachedBusinessFailure decodeIfPresent(Object cachedResponse) {
        if (!(cachedResponse instanceof String encoded) || !encoded.startsWith(MARKER)) {
            return null;
        }
        String payload = encoded.substring(MARKER.length());
        int separatorIndex = payload.indexOf(SEPARATOR);
        String className = separatorIndex >= 0 ? payload.substring(0, separatorIndex) : payload;
        String message = separatorIndex >= 0 ? payload.substring(separatorIndex + SEPARATOR.length()) : "";
        return new CachedBusinessFailure(className, message.isEmpty() ? null : message);
    }
}
