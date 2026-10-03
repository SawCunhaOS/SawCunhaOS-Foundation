
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

package br.com.sawcunhaos.foundation.jdempotent.core.chain;


import br.com.sawcunhaos.foundation.jdempotent.core.model.ChainData;
import br.com.sawcunhaos.foundation.jdempotent.core.model.KeyValuePair;

/**
 * Elo de uma cadeia de responsabilidade que decide o que um campo do payload contribui para a
 * chave de idempotência.
 *
 * <p>A cadeia montada pelo {@code IdempotentAspect} avalia, nesta ordem: {@code @JdempotentIgnore},
 * {@code @JdempotentId}, {@code @JdempotentProperty} e, por fim, o comportamento padrão (nome do
 * campo e valor).</p>
 */
public abstract class AnnotationChain {
    protected AnnotationChain nextChain;

    /**
     * Processa o campo ou delega ao próximo elo.
     *
     * @param chainData campo e objeto que o contém
     * @return o par nome/valor do campo; par sem chave exclui o campo da composição
     * @throws IllegalAccessException se o valor do campo não puder ser lido
     */
    public abstract KeyValuePair process(ChainData chainData) throws IllegalAccessException;

    /**
     * Define o próximo elo da cadeia.
     *
     * @param nextChain elo que recebe o campo quando este não o resolve
     */
    public void next(AnnotationChain nextChain) {
        this.nextChain = nextChain;
    }
}
