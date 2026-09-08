
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
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentId;

import java.lang.reflect.Field;

/**
 * Story 3.12 (AC #1): a field annotated with {@code @JdempotentId} exists only to receive the
 * generated idempotency key back (see {@code IdempotentAspect#setJdempotentId}) — it must never
 * influence the hash that produces that same key. This link excludes it from key composition
 * the same way {@link JdempotentIgnoreAnnotationChain} excludes {@code @JdempotentIgnore}
 * fields: by returning an empty {@link KeyValuePair}, which the caller's blank-key filter
 * (see {@code IdempotentAspect#getIdempotentNonIgnorableWrapper}) then skips entirely.
 */
public class JdempotentIdAnnotationChain extends AnnotationChain {
    @Override
    public KeyValuePair process(ChainData chainData) throws IllegalAccessException {
        Field declaredField = chainData.getDeclaredField();
        declaredField.setAccessible(true);
        JdempotentId annotation = declaredField.getAnnotation(JdempotentId.class);
        if (annotation != null) {
            return new KeyValuePair();
        }
        return super.nextChain.process(chainData);
    }
}
