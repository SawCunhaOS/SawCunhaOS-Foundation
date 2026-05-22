
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
