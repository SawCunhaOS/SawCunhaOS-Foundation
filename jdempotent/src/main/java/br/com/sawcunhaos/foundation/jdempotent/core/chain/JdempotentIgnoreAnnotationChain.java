
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
import br.com.sawcunhaos.foundation.utils.annotation.jdempotent.JdempotentIgnore;

import java.lang.reflect.Field;

public class JdempotentIgnoreAnnotationChain extends AnnotationChain {
    @Override
    public KeyValuePair process(ChainData chainData) throws IllegalAccessException {
        Field declaredField = chainData.getDeclaredField();
        declaredField.setAccessible(true);
        JdempotentIgnore annotation = declaredField.getAnnotation(JdempotentIgnore.class);
        if(annotation != null){
            return new KeyValuePair();
        }
        return super.nextChain.process(chainData);
    }
}
