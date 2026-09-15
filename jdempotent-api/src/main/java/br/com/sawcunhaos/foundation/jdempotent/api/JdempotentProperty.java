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

package br.com.sawcunhaos.foundation.jdempotent.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Renames the annotated field's entry in the idempotency key hash: {@link #value()} replaces the
 * field's own name as the key, while the field's actual value is still hashed. Two fields sharing
 * the same {@link #value()} on the same payload collide silently in that key, so keep values
 * unique per payload.
 *
 * <p>Leaving {@link #value()} at its default ({@code ""}) has the same practical effect as
 * {@link JdempotentIgnore}: a blank key is dropped from the hash material entirely, so the field
 * is excluded rather than keyed by an empty string or by its own name.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JdempotentProperty {

    /**
     * Key used in place of the field's own name in the idempotency key hash. Left at the
     * default, the field is excluded from the hash altogether — see the type-level Javadoc.
     *
     * @return the key to substitute for the field's own name, or blank to exclude the field
     */
    String value() default "";
}
