
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

import java.io.Serializable;
import java.time.Duration;

/**
 * Result of {@link br.com.sawcunhaos.foundation.jdempotent.core.datasource.IdempotentRepository#tryAcquire(IdempotencyKey, String, Duration)}.
 *
 * <p>Tells the caller whether it obtained the idempotency lock atomically, and,
 * when it did not, carries what is currently stored under the key so the caller
 * can decide what to do next. Check {@link #isMismatch()} <strong>first</strong>,
 * before either of the two cases below — a mismatch means the key is holding a
 * different payload, so neither replaying the cache nor signalling plain
 * "in progress" is correct (Story 3.6, AC #1/#2):</p>
 * <ul>
 *   <li>{@link #isMismatch()} true: the stored payload hash differs from this
 *       call's — a collision under the same key, not a genuine retry; the caller
 *       must signal that instead of doing either of the below (see
 *       {@code IdempotentPayloadMismatchException}).</li>
 *   <li>{@link #isMismatch()} false and {@link #getExistingResponse()} non-null:
 *       the earlier call already finished, its cached response should be
 *       replayed (the classic idempotent-retry case).</li>
 *   <li>{@link #isMismatch()} false and {@link #getExistingResponse()} null:
 *       the earlier call is still in flight, the caller should signal a conflict
 *       (see {@code IdempotentInProgressException}).</li>
 * </ul>
 *
 * <p>{@link #getExistingPayloadHash()} lets the caller compare it against this
 * call's {@link #getPayloadHash()} to tell a genuine duplicate apart from a
 * payload collision under the same idempotency key, without requiring another
 * round trip to the repository.</p>
 */
@Getter
public class Lease implements Serializable {

    private final IdempotencyKey key;
    private final boolean acquired;
    private final boolean mismatch;
    private final String payloadHash;
    private final Duration ttl;
    private final String existingPayloadHash;
    private final IdempotentResponseWrapper existingResponse;

    private Lease(IdempotencyKey key, boolean acquired, boolean mismatch, String payloadHash, Duration ttl,
                   String existingPayloadHash, IdempotentResponseWrapper existingResponse) {
        this.key = key;
        this.acquired = acquired;
        this.mismatch = mismatch;
        this.payloadHash = payloadHash;
        this.ttl = ttl;
        this.existingPayloadHash = existingPayloadHash;
        this.existingResponse = existingResponse;
    }

    /**
     * The lock was obtained atomically: this call owns the key until {@code ttl}
     * elapses or the response is stored.
     */
    public static Lease acquired(IdempotencyKey key, String payloadHash, Duration ttl) {
        return new Lease(key, true, false, payloadHash, ttl, null, null);
    }

    /**
     * The lock was NOT obtained: another call already holds (or held) it, with the
     * same payload hash.
     *
     * @param existingPayloadHash payload hash stored by the call that holds/held the key, may be null when unknown
     * @param existingResponse    cached response of the call that holds/held the key; null while it is still in progress
     */
    public static Lease inProgress(IdempotencyKey key, String payloadHash, Duration ttl,
                                    String existingPayloadHash, IdempotentResponseWrapper existingResponse) {
        return new Lease(key, false, false, payloadHash, ttl, existingPayloadHash, existingResponse);
    }

    /**
     * The lock was NOT obtained, and what is stored under the key was acquired with a
     * <strong>different</strong> payload hash (Story 3.6): a collision under the same
     * idempotency key, not a genuine retry of the same call. Applies whether the other
     * call is still in progress or already finished with a cached response — either way
     * the caller must not treat this as a duplicate/in-progress call.
     *
     * @param existingPayloadHash payload hash stored by the call that holds/held the key
     * @param existingResponse    cached response of the call that holds/held the key; null while it is still in progress
     */
    public static Lease mismatch(IdempotencyKey key, String payloadHash, Duration ttl,
                                  String existingPayloadHash, IdempotentResponseWrapper existingResponse) {
        return new Lease(key, false, true, payloadHash, ttl, existingPayloadHash, existingResponse);
    }

    /**
     * True when the previous holder of this key already finished and cached a
     * response that can be replayed instead of re-running the protected method.
     */
    public boolean hasCachedResponse() {
        return existingResponse != null;
    }

    @Override
    public String toString() {
        return String.format("Lease [key=%s, acquired=%s, mismatch=%s, hasCachedResponse=%s]", key, acquired, mismatch, hasCachedResponse());
    }
}
