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

package br.com.sawcunhaos.foundation.jdempotent.api;

/**
 * Declares the intended policy for when a client-supplied {@code Idempotency-Key} header
 * (Story 3.13, {@link KeySource#HEADER_THEN_FIELDS}) is reused across requests whose annotated
 * fields (request payload) differ — a client resending the same header with a different body.
 *
 * <p><b>Scope note (Story 3.13):</b> this attribute exists so {@code @JdempotentResource} can
 * declare the policy, but no code in this module currently branches on its value. A header key
 * reused with a different payload is already caught today by the existing, key-source-agnostic
 * payload-mismatch check in {@code IdempotentAspect#execute()} (Story 3.6 —
 * {@code idempotentRepository.tryAcquire(key, payloadHash, ttl)} compares the incoming payload
 * hash against the one already stored under the key, regardless of how the key itself was
 * derived) — it throws {@code IdempotentPayloadMismatchException} exactly in this scenario,
 * without any change from this story. Integration modules map that today to
 * {@code 422 Unprocessable Entity}/{@code PAYLOAD_MISMATCH} (see that exception's Javadoc).</p>
 *
 * <p>{@code CONFLICT} is deliberately a different name, not a reuse of
 * {@code PAYLOAD_MISMATCH}: a reused {@code Idempotency-Key} header colliding with a different
 * body is conventionally surfaced as {@code 409 Conflict} by APIs that implement this header
 * pattern (e.g. Stripe's Idempotency-Key semantics), as opposed to Story 3.6's generic
 * {@code 422}. A future story may use this attribute to have {@code IdempotentAspect} raise a
 * distinct, header-specific conflict signal instead of reusing
 * {@code IdempotentPayloadMismatchException} — not implemented here because no acceptance
 * criterion or task of Story 3.13 requires a different exception type, only that the attribute
 * exist and be documented (see Story 3.13 Dev Notes).</p>
 */
public enum IdempotentKeyMismatchPolicy {

    /**
     * Only value today (see class Javadoc): a header key reused with a different payload is a
     * conflict, currently surfaced via the pre-existing Story 3.6 payload-mismatch mechanism.
     */
    CONFLICT
}
