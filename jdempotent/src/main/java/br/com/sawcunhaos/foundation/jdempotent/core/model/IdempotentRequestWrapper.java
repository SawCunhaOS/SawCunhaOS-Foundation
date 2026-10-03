
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
 * Payload da requisição que compõe a chave de idempotência.
 *
 * <p>O {@code toString()} concatena os elementos ordenados, para que a ordem dos argumentos não
 * altere o hash.</p>
 */
@Setter
@Getter
@NoArgsConstructor
@SuppressWarnings("serial")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Mutable serializable DTO (Lombok @Getter/@Setter) shared across the idempotency pipeline by design.")
public class IdempotentRequestWrapper implements Serializable {
    private List<Object> request;

    /**
     * Cria o wrapper com um único elemento.
     *
     * @param request o valor do payload
     */
    public IdempotentRequestWrapper(Object request) {
        this.request = Collections.singletonList(request);
    }

    /**
     * Cria o wrapper com uma lista de elementos.
     *
     * @param request os valores do payload
     */
    public IdempotentRequestWrapper(List<Object> request) {
        this.request = request;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(request);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IdempotentRequestWrapper)) {
            return false;
        }
        IdempotentRequestWrapper other = (IdempotentRequestWrapper) obj;
        return Objects.equals(request, other.request);
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
