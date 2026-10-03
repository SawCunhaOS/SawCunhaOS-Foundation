
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
 * Resposta do método protegido guardada no cache de idempotência.
 *
 * <p>O campo é {@code Object}: no Redis, só o tipo da raiz do valor serializado é preservado, então
 * um POJO aninhado volta como {@code LinkedHashMap}. Por isso a falha da política
 * {@code KEEP_FAILED} é guardada como {@code String} (ver {@link CachedBusinessFailure}).</p>
 */
@Getter
@NoArgsConstructor
@SuppressWarnings("serial")
public class IdempotentResponseWrapper implements Serializable {

    private Object response;

    /**
     * Cria o wrapper com a resposta.
     *
     * @param response valor devolvido pelo método protegido
     */
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
