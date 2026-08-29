
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
 * can decide what to do next:</p>
 * <ul>
 *   <li>{@link #getExistingResponse()} non-null: the earlier call already finished,
 *       its cached response should be replayed (the classic idempotent-retry case).</li>
 *   <li>{@link #getExistingResponse()} null: the earlier call is still in flight,
 *       the caller should signal a conflict (see {@code IdempotentInProgressException}).</li>
 * </ul>
 *
 * <p>{@link #getExistingPayloadHash()} is exposed so Story 3.6 can compare it
 * against this call's {@link #getPayloadHash()} to tell a genuine duplicate
 * apart from a payload collision under the same idempotency key, without
 * requiring another round trip to the repository.</p>
 */
@Getter
public class Lease implements Serializable {

    private final IdempotencyKey key;
    private final boolean acquired;
    private final String payloadHash;
    private final Duration ttl;
    private final String existingPayloadHash;
    private final IdempotentResponseWrapper existingResponse;

    private Lease(IdempotencyKey key, boolean acquired, String payloadHash, Duration ttl,
                   String existingPayloadHash, IdempotentResponseWrapper existingResponse) {
        this.key = key;
        this.acquired = acquired;
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
        return new Lease(key, true, payloadHash, ttl, null, null);
    }

    /**
     * The lock was NOT obtained: another call already holds (or held) it.
     *
     * @param existingPayloadHash payload hash stored by the call that holds/held the key, may be null when unknown
     * @param existingResponse    cached response of the call that holds/held the key; null while it is still in progress
     */
    public static Lease inProgress(IdempotencyKey key, String payloadHash, Duration ttl,
                                    String existingPayloadHash, IdempotentResponseWrapper existingResponse) {
        return new Lease(key, false, payloadHash, ttl, existingPayloadHash, existingResponse);
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
        return String.format("Lease [key=%s, acquired=%s, hasCachedResponse=%s]", key, acquired, hasCachedResponse());
    }
}
