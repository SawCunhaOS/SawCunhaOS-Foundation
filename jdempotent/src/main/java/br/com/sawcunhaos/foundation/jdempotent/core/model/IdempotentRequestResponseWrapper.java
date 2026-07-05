
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

    public IdempotentRequestResponseWrapper(IdempotentRequestWrapper request) {
        this.request = request;
    }

    public IdempotentRequestResponseWrapper(IdempotentRequestWrapper request, IdempotentResponseWrapper response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public String toString() {
        return String.format("IdempotentRequestResponseWrapper [request=%s, response=%s]", request, response);
    }
}
