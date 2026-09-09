
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 *
 *  That is a container for idempotent requests and responses
 *
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("serial")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Mutable serializable DTO (Lombok @Getter/@Setter); the wrapped request/response are shared across the idempotency pipeline by design.")
public class IdempotentRequestResponseWrapper implements Serializable {
    private IdempotentRequestWrapper request;
    private IdempotentResponseWrapper response = null;

    /**
     * Hash of the payload that acquired the lease for this key (Story 3.5).
     * Populated by {@code tryAcquire}; used by Story 3.6 to tell a genuine
     * duplicate call apart from a different payload colliding on the same key.
     */
    private String payloadHash;

    /**
     * Instant this entry stops being valid, or {@code null} if it never expires
     * (Story 3.15). Only meaningful for the in-memory repository — Redis enforces
     * TTL natively at the key level and does not read this field.
     */
    private Instant expiresAt;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public IdempotentRequestResponseWrapper(IdempotentRequestWrapper request) {
        this.request = request;
    }

    public IdempotentRequestResponseWrapper(IdempotentRequestWrapper request, IdempotentResponseWrapper response) {
        this.request = request;
        this.response = response;
    }

    public IdempotentRequestResponseWrapper(IdempotentRequestWrapper request, String payloadHash) {
        this.request = request;
        this.payloadHash = payloadHash;
    }

    @Override
    public String toString() {
        return String.format("IdempotentRequestResponseWrapper [request=%s, response=%s]", request, response);
    }
}
