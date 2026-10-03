
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

package br.com.sawcunhaos.foundation.jdempotent.core.callback;

/**
 * Callback que permite tratar como erro uma resposta "bem-sucedida" do método protegido.
 *
 * <p>Se {@link #onErrorCondition(Object)} devolver {@code true}, o aspecto remove a chave do
 * repositório (liberando um retry) e lança a exceção de {@link #onErrorCustomException()}.</p>
 */
public interface ErrorConditionalCallback {

    /**
     * Indica se a resposta representa uma condição de erro.
     *
     * @param response valor devolvido pelo método protegido
     * @return {@code true} para tratar a resposta como erro
     */
    boolean onErrorCondition(Object response);

    /**
     * Exceção lançada quando {@link #onErrorCondition(Object)} devolve {@code true}.
     *
     * @return a exceção a lançar
     */
    RuntimeException onErrorCustomException();

}
