
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

package br.com.sawcunhaos.foundation.jdempotent.core.datasource;

import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.Lease;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * an interface that the functionality required of a request store for idempotent method invocations.
 */
public interface IdempotentRepository {
    /**
     * @param key
     * @return
     */
    boolean contains(IdempotencyKey key);

    /**
     * Atomically tries to acquire the idempotency lock for {@code key} (Story 3.5).
     * Replaces the non-atomic {@code contains() -> store()} sequence: exactly one
     * concurrent caller for the same key gets a {@link Lease} with {@code acquired == true},
     * every other concurrent caller gets a {@link Lease} describing what is already
     * stored for that key (an in-progress call, a finished one with a cached response,
     * or, when the stored {@code payloadHash} differs from this call's, a payload
     * collision — {@link Lease#isMismatch()}, Story 3.6). The mismatch comparison is
     * done against the same value the lock-acquisition step already reads/writes, so
     * implementations must not perform it as a second round trip to the store.
     *
     * @param key         the idempotency key
     * @param payloadHash hash of the request payload, stored alongside the lease and
     *                    compared against the hash already stored under the key (if any)
     *                    to detect payload collisions (Story 3.6)
     * @param ttl         how long the lease is held before it expires; a zero/negative
     *                    duration lets the implementation fall back to its own default
     * @return the lease
     */
    Lease tryAcquire(IdempotencyKey key, String payloadHash, Duration ttl);

    /**
     * Checks the cache for an existing call for this request
     *
     * @param key
     * @return
     */
    IdempotentResponseWrapper getResponse(IdempotencyKey key);

    /**
     *
     * @param key
     * @param requestObject
     * @param ttl
     * @param timeUnit
     */
    void store(IdempotencyKey key, IdempotentRequestWrapper requestObject,Long ttl, TimeUnit timeUnit);


    /**
     * @param key
     */
    void remove(IdempotencyKey key);

    /**
     * @param request
     * @param idempotentResponse
     */
    void setResponse(IdempotencyKey key, IdempotentRequestWrapper request, IdempotentResponseWrapper idempotentResponse, Long ttl, TimeUnit timeUnit);
}
