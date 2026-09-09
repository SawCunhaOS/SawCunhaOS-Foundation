
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
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Includes all the methods of IdempotentRequestStore
 */
public abstract class AbstractIdempotentRepository implements IdempotentRepository {

    /**
     * Side effect (Story 3.15 code review): as well as reading, this may lazily evict the
     * entry under {@code key} from the backing map, if it is found expired. Callers never
     * observe an expired entry either way, but the map itself may end up smaller as a result
     * of a call that looks read-only.
     */
    @Override
    public boolean contains(IdempotencyKey key) {
        return getIfNotExpired(key) != null;
    }

    /**
     * Side effect (Story 3.15 code review): see {@link #contains(IdempotencyKey)} — this may
     * also lazily evict an expired entry as part of the read.
     */
    @Override
    public IdempotentResponseWrapper getResponse(IdempotencyKey key) {
        IdempotentRequestResponseWrapper wrapper = getIfNotExpired(key);
        return wrapper != null ? wrapper.getResponse() : null;
    }

    @Override
    public void store(IdempotencyKey key, IdempotentRequestWrapper request,Long ttl, TimeUnit timeUnit) {
        IdempotentRequestResponseWrapper wrapper = new IdempotentRequestResponseWrapper(request);
        wrapper.setExpiresAt(computeExpiresAt(ttl, timeUnit));
        getMap().put(key, wrapper);
    }

    /**
     * Story 3.15 code review: mutates the existing wrapper (response + refreshed
     * {@code expiresAt}) from inside {@code computeIfPresent}'s remapping function, not via a
     * separate {@code get()} then {@code put()} — {@code IdempotentRequestResponseWrapper} has
     * no {@code equals()}/{@code hashCode()} override, so a plain get-then-mutate-then-put
     * would give a concurrent {@link #getIfNotExpired} an identity-only snapshot to compare
     * against, which still "matches" after this method mutates that very same object in
     * place. Running under the map's own per-key lock (which {@code computeIfPresent}
     * shares with {@code get}/{@code getIfNotExpired}'s own {@code computeIfPresent}/{@code
     * tryAcquire}'s {@code compute} on the same key) closes that window entirely instead of
     * narrowing it.
     */
    @Override
    public void setResponse(IdempotencyKey key, IdempotentRequestWrapper request,
                            IdempotentResponseWrapper idempotentResponse, Long ttl, TimeUnit timeUnit) {
        getMap().computeIfPresent(key, (k, requestResponseWrapper) -> {
            requestResponseWrapper.setResponse(idempotentResponse);
            requestResponseWrapper.setExpiresAt(computeExpiresAt(ttl, timeUnit));
            return requestResponseWrapper;
        });
    }

    /**
     * Story 3.15: {@code ConcurrentHashMap} (the only backing store for this repository)
     * has no native entry expiration, so TTL is enforced lazily here — an expired entry
     * is treated as absent and evicted on the next {@code contains()}/{@code getResponse()}
     * that observes it, instead of a background sweep.
     *
     * <p><strong>Code review fix:</strong> the expiration check and the eviction used to be
     * two separate steps (a plain {@code get()}, then a compare-and-remove keyed off that
     * same, possibly since-mutated, object reference) — a window a concurrent
     * {@code setResponse()} could race, since it mutates the very same
     * {@code IdempotentRequestResponseWrapper} instance in place rather than replacing it
     * with a new one. {@code computeIfPresent} folds the read and the evict-if-expired
     * decision into one operation under the map's own per-key lock, so no other {@code
     * compute}/{@code computeIfPresent}/{@code put} call for this key (including {@code
     * setResponse()}'s own, see its Javadoc) can interleave mid-decision.</p>
     */
    private IdempotentRequestResponseWrapper getIfNotExpired(IdempotencyKey key) {
        return getMap().computeIfPresent(key, (k, wrapper) -> wrapper.isExpired() ? null : wrapper);
    }

    private static Instant computeExpiresAt(Long ttl, TimeUnit timeUnit) {
        if (ttl == null || ttl <= 0 || timeUnit == null) {
            return null;
        }
        return Instant.now().plusMillis(timeUnit.toMillis(ttl));
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
     * <p>The payload-collision check (Story 3.6, AC #1/#2) is done against that same
     * {@code existing} entry returned by {@code putIfAbsent} — no second map lookup —
     * so it takes precedence over both the cached-response and in-progress outcomes
     * within this one atomic call.</p>
     *
     * <p>NOTE: the placeholder inserted here carries no expiration of its own — it relies
     * on the follow-up {@code setResponse()} call (Story 3.15) to set the entry's real TTL
     * once the wrapped method finishes. If the caller crashes between {@code tryAcquire}
     * and {@code setResponse}, this in-progress entry never expires — add a scheduled
     * evictor if a long-lived in-memory idempotency window becomes a real requirement.</p>
     *
     * <p><strong>Story 3.15:</strong> a plain {@code putIfAbsent} would never reacquire a key
     * whose stored entry already expired — the entry is still physically present, so it
     * would be misread as an in-progress call or replayed as a cached response, silently
     * ignoring the configured TTL for exactly the {@code @JdempotentResource} -> {@code
     * IdempotentAspect.execute()} path this repository serves.</p>
     *
     * <p><strong>Code review fix:</strong> the initial fix for that (check {@code existing}
     * outside the map operation, then {@code replace(key, existing, placeholder)}) reopened
     * the same identity-vs-mutation race described on {@link #setResponse}'s Javadoc: a
     * concurrent {@code setResponse()} mutating {@code existing} in place between the
     * expiration check and the {@code replace} call would still "match" the CAS, discarding
     * a response that call had just legitimately (re)written. This now decides and installs
     * the placeholder atomically via {@code compute}, under the same per-key lock {@link
     * #getIfNotExpired}/{@code setResponse()} use, so the check and the swap can no longer be
     * observed as two separate steps by a concurrent caller.</p>
     */
    @Override
    public Lease tryAcquire(IdempotencyKey key, String payloadHash, Duration ttl) {
        IdempotentRequestResponseWrapper placeholder = new IdempotentRequestResponseWrapper(null, payloadHash);
        IdempotentRequestResponseWrapper current = getMap().compute(key,
                (k, existing) -> (existing == null || existing.isExpired()) ? placeholder : existing);
        if (current == placeholder) {
            return Lease.acquired(key, payloadHash, ttl);
        }
        String existingPayloadHash = current.getPayloadHash();
        if (existingPayloadHash != null && !existingPayloadHash.equals(payloadHash)) {
            return Lease.mismatch(key, payloadHash, ttl, existingPayloadHash, current.getResponse());
        }
        return Lease.inProgress(key, payloadHash, ttl, existingPayloadHash, current.getResponse());
    }


    /**
     * @return
     */
    protected abstract Map<IdempotencyKey, IdempotentRequestResponseWrapper> getMap();
}
