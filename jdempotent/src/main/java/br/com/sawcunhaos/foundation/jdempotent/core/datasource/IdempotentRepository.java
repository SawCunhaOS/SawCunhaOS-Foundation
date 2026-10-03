
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
import br.com.sawcunhaos.foundation.jdempotent.core.model.Lease;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Contrato do armazenamento usado pelo {@code IdempotentAspect} para controlar chamadas
 * idempotentes: lock atômico por chave, resposta em cache e remoção.
 *
 * <p>Implementações fornecidas: {@link InMemoryIdempotentRepository} (padrão, local à JVM) e
 * {@code RedisIdempotentRepository} (distribuído, fail-open com circuit breaker). O repositório é
 * um fast-path contra reprocessamento, não a garantia final contra duplicidade: essa é a constraint
 * {@code UNIQUE} do banco de dados do consumidor.</p>
 */
public interface IdempotentRepository {
    /**
     * Informa se há uma entrada para a chave.
     *
     * <p>Nota (Story 3.15): uma implementação que aplica o TTL de forma preguiçosa (sem expiração
     * nativa por entrada) pode remover uma entrada expirada como efeito colateral desta chamada;
     * o chamador só observa "ausente", mas o armazenamento pode encolher.</p>
     *
     * @param key chave de idempotência
     * @return {@code true} se há entrada válida (não expirada) para a chave
     */
    boolean contains(IdempotencyKey key);

    /**
     * Tenta adquirir atomicamente o lock de idempotência para a chave (Story 3.5).
     *
     * <p>Substitui a sequência não atômica {@code contains() -> store()}: exatamente um chamador
     * concorrente para a mesma chave recebe um {@link Lease} com {@code acquired == true}; os
     * demais recebem um {@link Lease} descrevendo o que já está armazenado (chamada em andamento,
     * chamada concluída com resposta em cache ou, quando o {@code payloadHash} armazenado difere
     * do desta chamada, colisão de payload, {@link Lease#isMismatch()}, Story 3.6). A comparação
     * de hash usa o mesmo valor lido/escrito pela aquisição do lock, então a implementação não
     * deve fazê-la em uma segunda ida ao armazenamento.</p>
     *
     * @param key         chave de idempotência
     * @param payloadHash hash do payload, gravado junto do lease e comparado com o hash já
     *                    gravado sob a chave (se houver) para detectar colisão de payload
     * @param ttl         por quanto tempo o lease é mantido; duração zero ou negativa faz a
     *                    implementação usar o seu padrão; no repositório em memória o lease não expira até {@code setResponse}
     * @return o lease (adquirido, em andamento ou colisão de payload)
     */
    Lease tryAcquire(IdempotencyKey key, String payloadHash, Duration ttl);

    /**
     * Consulta a resposta em cache de uma chamada anterior com esta chave.
     *
     * <p>Nota (Story 3.15): mesmo efeito colateral de {@link #contains(IdempotencyKey)}: uma
     * implementação com TTL preguiçoso pode remover aqui uma entrada expirada.</p>
     *
     * @param key chave de idempotência
     * @return a resposta em cache, ou {@code null} se ausente, expirada ou ainda em andamento
     */
    IdempotentResponseWrapper getResponse(IdempotencyKey key);

    /**
     * Grava a requisição sob a chave (sem resposta ainda). O aspecto não a usa mais:
     * prefira {@link #tryAcquire}, que adquire e grava de forma atômica.
     *
     * @param key           chave de idempotência
     * @param requestObject payload da requisição
     * @param ttl           validade da entrada; {@code 0} usa o padrão da implementação
     * @param timeUnit      unidade de {@code ttl}
     */
    void store(IdempotencyKey key, IdempotentRequestWrapper requestObject, Long ttl, TimeUnit timeUnit);

    /**
     * Remove a entrada da chave, liberando-a para uma nova execução (política
     * {@code RELEASE} ou condição de erro do {@code ErrorConditionalCallback}).
     *
     * @param key chave de idempotência
     */
    void remove(IdempotencyKey key);

    /**
     * Grava a resposta final sob a chave, preservando o {@code payloadHash} gravado por
     * {@link #tryAcquire} e renovando a validade da entrada.
     *
     * @param key                chave de idempotência
     * @param request            payload da requisição
     * @param idempotentResponse resposta (ou falha codificada, política {@code KEEP_FAILED})
     * @param ttl                validade da entrada; {@code 0} usa o padrão da implementação
     * @param timeUnit           unidade de {@code ttl}
     */
    void setResponse(IdempotencyKey key, IdempotentRequestWrapper request,
                     IdempotentResponseWrapper idempotentResponse, Long ttl, TimeUnit timeUnit);
}
