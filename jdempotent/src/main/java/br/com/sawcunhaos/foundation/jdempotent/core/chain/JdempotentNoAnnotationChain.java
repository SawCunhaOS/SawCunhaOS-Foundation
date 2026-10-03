
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

import java.lang.reflect.Field;

/**
 * Elo para campos sem nenhuma anotação: usa o nome do campo e o seu valor.
 *
 * <p>Não é o elo de partida da cadeia montada por {@code IdempotentAspect}: o resultado seria o
 * mesmo do {@link JdempotentDefaultChain}, que já cobre o campo sem anotação.</p>
 */
public class JdempotentNoAnnotationChain extends AnnotationChain {

    @Override
    public KeyValuePair process(ChainData chainData) throws IllegalAccessException {
        if (chainData.getDeclaredField().getDeclaredAnnotations().length == 0) {
            Field declaredField = chainData.getDeclaredField();
            declaredField.setAccessible(true);
            return new KeyValuePair(declaredField.getName(),declaredField.get(chainData.getArgs()));
        }
        return super.nextChain.process(chainData);
    }
}
