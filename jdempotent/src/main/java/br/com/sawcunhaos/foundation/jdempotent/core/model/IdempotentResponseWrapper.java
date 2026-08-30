
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
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Wraps the incoming event response
 *
 */
@Getter
@NoArgsConstructor
@SuppressWarnings("serial")
public class IdempotentResponseWrapper implements Serializable {

    private Object response;

    public IdempotentResponseWrapper(Object response) {
        this.response = response;
    }

    @Override
    public int hashCode() {
        return response == null ? 0 : response.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IdempotentResponseWrapper other)) {
            return false;
        }
        return Objects.equals(response, other.response);
    }

    @Override
    public String toString() {
        return String.format("IdempotentResponseWrapper [response=%s]", response);
    }
}
