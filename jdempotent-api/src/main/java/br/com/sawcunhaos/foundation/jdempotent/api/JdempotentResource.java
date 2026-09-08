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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;

/**
 * Add to the methods that need to be idempotent.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JdempotentResource {

    /**
     * Prefix value to make hash value more collider
     *
     * @return
     */
    String cachePrefix() default "";

    /**
     * To add custom ttl
     *
     * @return
     */
    long ttl() default 0L;

    /**
     * To add custom time unit
     *
     * @return
     */
    TimeUnit ttlTimeUnit() default TimeUnit.HOURS;

    /**
     * What happens to the idempotency key when this method throws a business
     * exception: {@link IdempotentFailurePolicy#RELEASE} (default, removes the
     * key so a retry re-executes the method) or
     * {@link IdempotentFailurePolicy#KEEP_FAILED} (records the exception as the
     * cached result, so a retry replays the same failure instead of
     * re-executing the method).
     *
     * @return
     */
    IdempotentFailurePolicy onBusinessException() default IdempotentFailurePolicy.RELEASE;

    /**
     * Where the idempotency key is sourced from (Story 3.13). Defaults to
     * {@link KeySource#FIELDS_ONLY}, preserving the behavior from before this attribute existed.
     * {@link KeySource#HEADER_THEN_FIELDS} reads the header named by {@link #headerName()} first.
     *
     * @return
     */
    KeySource keySource() default KeySource.FIELDS_ONLY;

    /**
     * Name of the HTTP request header carrying the client-supplied idempotency key, read only
     * when {@link #keySource()} is {@link KeySource#HEADER_THEN_FIELDS} (Story 3.13). Never
     * defaults to {@code X-Request-ID}: that header identifies a request for
     * correlation/tracing, a different concern from business idempotency, and is never read as a
     * fallback for this attribute.
     *
     * @return
     */
    String headerName() default "";

    /**
     * Declares the intended policy for a header/payload mismatch (Story 3.13) — see
     * {@link IdempotentKeyMismatchPolicy} for exactly what is (and isn't) wired to this attribute
     * today.
     *
     * @return
     */
    IdempotentKeyMismatchPolicy onMismatch() default IdempotentKeyMismatchPolicy.CONFLICT;
}
