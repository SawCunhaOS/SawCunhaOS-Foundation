
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *
 * Wraps the incoming event value
 *
 */
@Setter
@Getter
@NoArgsConstructor
@SuppressWarnings("serial")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2", "EQ_UNUSUAL"},
        justification = "Mutable serializable DTO (Lombok @Getter/@Setter) shared across the idempotency pipeline by design (EI_EXPOSE_REP2). "
                + "equals() intentionally matches the wrapped request elements for idempotency-key comparison; changing it would alter dedup behavior (EQ_UNUSUAL).")
public class IdempotentRequestWrapper implements Serializable {
    private List<Object> request;

    public IdempotentRequestWrapper(Object request) {
        this.request = Collections.singletonList(request);
    }

    public IdempotentRequestWrapper(List<Object> request) {
        this.request = request;
    }

    @Override
    public int hashCode() {
        return request == null ? 0 : request.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return !Objects.isNull(request) && request.stream().anyMatch(req -> req.equals(obj));
    }

    @Override
    public String toString() {
        StringBuilder requestBuilder = new StringBuilder();
        this.request.stream()
                .map(Object::toString)
                .toList().stream()
                .sorted(String::compareTo)
                .forEach(requestBuilder::append);

        return String.format("IdempotentRequestWrapper [request=%s]", requestBuilder);
    }
}
