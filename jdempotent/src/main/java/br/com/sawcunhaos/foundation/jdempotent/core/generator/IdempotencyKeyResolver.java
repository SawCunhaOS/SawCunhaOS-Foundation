
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

import br.com.sawcunhaos.foundation.jdempotent.core.constant.CryptographyAlgorithm;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;

import java.security.MessageDigest;

/**
 * Story 3.12 (AC #2): the single point that turns an already-collected
 * {@link IdempotentRequestWrapper} into the final {@link IdempotencyKey}. Canonical field
 * composition (deterministic {@code TreeMap} ordering, {@code @JdempotentId} exclusion)
 * happens upstream, in the {@code annotationChain} that builds the wrapper — see
 * {@code IdempotentAspect#getIdempotentNonIgnorableWrapper} — so by the time a wrapper reaches
 * this class it is already canonical; this class' job is hashing it into a key.
 *
 * <p>Deliberately takes no {@code ProceedingJoinPoint}/AspectJ type — only plain data (a
 * request wrapper and a listener/prefix name) — so it is reusable, unchanged, by any future
 * entrypoint (HTTP via {@code IdempotentAspect} today, messaging tomorrow per Story 3.13)
 * without duplicating key-composition logic.</p>
 *
 * <p>Delegates the hash+prefix mechanics to the {@link KeyGenerator} supplied at construction
 * (a {@link DefaultKeyGenerator} in practice) so that already-tested mechanism keeps behaving
 * identically (NFR4) — this class additionally owns the digest/algorithm plumbing so callers
 * never have to manage a {@link MessageDigest} themselves.</p>
 */
public class IdempotencyKeyResolver {

    private final KeyGenerator keyGenerator;

    /**
     * Story 3.12 (patch): mirrors {@code IdempotentAspect}'s own {@code StringBuilder} pool so
     * this resolver doesn't reallocate one per call — kept private to this class (rather than
     * shared with the aspect) so the resolver stays free of any dependency on {@code
     * IdempotentAspect}, per AC #2.
     */
    private static final ThreadLocal<StringBuilder> stringBuilders =
            new ThreadLocal<>() {
                @Override
                protected StringBuilder initialValue() {
                    return new StringBuilder();
                }

                @Override
                public StringBuilder get() {
                    StringBuilder builder = super.get();
                    builder.setLength(0);
                    return builder;
                }
            };

    public IdempotencyKeyResolver() {
        this(new DefaultKeyGenerator());
    }

    public IdempotencyKeyResolver(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    public IdempotencyKey resolve(IdempotentRequestWrapper requestObject, String listenerName) {
        MessageDigest messageDigest = CryptographyAlgorithm.SHA256.newDigest();
        return keyGenerator.generateIdempotentKey(requestObject, listenerName, stringBuilders.get(), messageDigest);
    }
}
