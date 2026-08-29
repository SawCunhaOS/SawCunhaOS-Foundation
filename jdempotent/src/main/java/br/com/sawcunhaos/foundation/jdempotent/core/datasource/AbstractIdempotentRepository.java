
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
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.Lease;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Includes all the methods of IdempotentRequestStore
 */
public abstract class AbstractIdempotentRepository implements IdempotentRepository {

    @Override
    public boolean contains(IdempotencyKey key) {
        return getMap().containsKey(key);
    }

    @Override
    public IdempotentResponseWrapper getResponse(IdempotencyKey key) {
        return getMap().containsKey(key) ? getMap().get(key).getResponse() : null;
    }

    @Override
    public void store(IdempotencyKey key, IdempotentRequestWrapper request,Long ttl, TimeUnit timeUnit) {
        getMap().put(key, new IdempotentRequestResponseWrapper(request));
    }

    @Override
    public void setResponse(IdempotencyKey key, IdempotentRequestWrapper request,
                            IdempotentResponseWrapper idempotentResponse, Long ttl, TimeUnit timeUnit) {
        if (getMap().containsKey(key)) {
            IdempotentRequestResponseWrapper requestResponseWrapper = getMap().get(key);
            requestResponseWrapper.setResponse(idempotentResponse);
            getMap().put(key, requestResponseWrapper);
        }
    }

    @Override
    public void remove(IdempotencyKey key) {
        getMap().remove(key);
    }

    /**
     * {@code ConcurrentHashMap.putIfAbsent} is a single atomic operation: exactly one
     * concurrent caller inserts the placeholder entry and gets {@code acquired == true},
     * every other concurrent caller observes the entry the winner just inserted (or an
     * already-finished one) and gets it back via the {@link Lease}.
     *
     * <p>NOTE: {@code ttl} is not enforced here (entries never expire), same as the
     * pre-existing {@code store()}/{@code setResponse()} for this in-memory implementation
     * — add a scheduled evictor if a long-lived in-memory idempotency window becomes a
     * real requirement.</p>
     */
    @Override
    public Lease tryAcquire(IdempotencyKey key, String payloadHash, Duration ttl) {
        IdempotentRequestResponseWrapper placeholder = new IdempotentRequestResponseWrapper(null, payloadHash);
        IdempotentRequestResponseWrapper existing = getMap().putIfAbsent(key, placeholder);
        if (existing == null) {
            return Lease.acquired(key, payloadHash, ttl);
        }
        return Lease.inProgress(key, payloadHash, ttl, existing.getPayloadHash(), existing.getResponse());
    }


    /**
     * @return
     */
    protected abstract Map<IdempotencyKey, IdempotentRequestResponseWrapper> getMap();
}
