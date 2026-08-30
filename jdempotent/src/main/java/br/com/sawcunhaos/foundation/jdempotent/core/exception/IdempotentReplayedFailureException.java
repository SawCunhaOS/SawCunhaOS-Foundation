
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

package br.com.sawcunhaos.foundation.jdempotent.core.exception;

import br.com.sawcunhaos.foundation.jdempotent.core.model.CachedBusinessFailure;
import lombok.Getter;

/**
 * Thrown by {@code IdempotentAspect} when a retry hits a key whose earlier
 * call failed under {@code IdempotentFailurePolicy#KEEP_FAILED} (Story 3.8).
 *
 * <p>This is not the original exception instance or type: only the original
 * exception's class name and message survive the round trip through the
 * idempotency repository (see {@link CachedBusinessFailure}). Callers that
 * need to know what originally failed should inspect
 * {@link #getOriginalExceptionClassName()} and {@link #getMessage()}.</p>
 */
@Getter
public class IdempotentReplayedFailureException extends RuntimeException {

    private final String originalExceptionClassName;

    public IdempotentReplayedFailureException(CachedBusinessFailure cachedFailure) {
        super(cachedFailure.getExceptionClassName() + ": " + cachedFailure.getExceptionMessage());
        this.originalExceptionClassName = cachedFailure.getExceptionClassName();
    }
}
