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

/**
 * Declares where {@link JdempotentResource} sources the idempotency key from (Story 3.13).
 */
public enum KeySource {

    /**
     * Default. Preserves the pre-Story-3.13 behavior: the key is composed exclusively from the
     * annotated fields ({@link JdempotentProperty}, or every non-ignored field when none is
     * declared), then hashed by the configured key generator.
     */
    FIELDS_ONLY,

    /**
     * Precedence is header &gt; annotated fields &gt; hash. When the HTTP request header named by
     * {@link JdempotentResource#headerName()} is present, its value is the source of the key —
     * the annotated fields are not consulted at all in that case. Falls back to
     * {@link #FIELDS_ONLY} behavior when the header is absent/blank, or when there is no HTTP
     * request context to read it from (e.g. a messaging listener invoking the annotated method
     * outside a {@code DispatcherServlet}) — never throws in that case.
     */
    HEADER_THEN_FIELDS
}
