
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

package br.com.sawcunhaos.foundation.jdempotent.core.generator;

import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;

import java.security.MessageDigest;

/**
 * Estratégia de composição da chave de idempotência a partir do payload da requisição.
 */
public interface KeyGenerator {

    /**
     * Gera a chave de idempotência.
     *
     * @param requestObject payload já coletado e canônico
     * @param listenerName  prefixo de cache ({@code cachePrefix} do {@code @JdempotentResource})
     * @param builder       {@link StringBuilder} reutilizável para montar a chave
     * @param messageDigest digest usado para o hash do payload
     * @return a chave de idempotência
     */
    IdempotencyKey generateIdempotentKey(IdempotentRequestWrapper requestObject, String listenerName, StringBuilder builder, MessageDigest messageDigest);

}
