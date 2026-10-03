
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositório em memória, padrão do {@code IdempotentAspect}, apoiado em um
 * {@link ConcurrentHashMap}.
 *
 * <p>Local à JVM: não deduplica entre instâncias da aplicação e perde o estado ao reiniciar. Útil
 * em testes e uso programático; para ambientes com mais de uma instância use o repositório Redis.</p>
 */
public class InMemoryIdempotentRepository extends AbstractIdempotentRepository {

    private final ConcurrentHashMap<IdempotencyKey, IdempotentRequestResponseWrapper> map;

    /** Cria um repositório vazio. */
    public InMemoryIdempotentRepository() {
        this.map = new ConcurrentHashMap<>();
    }

    @Override
    protected Map<IdempotencyKey, IdempotentRequestResponseWrapper> getMap() {
        return map;
    }

}
