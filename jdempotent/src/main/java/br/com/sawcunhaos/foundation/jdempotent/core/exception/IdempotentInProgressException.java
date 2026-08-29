
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

import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import lombok.Getter;

/**
 * Thrown by {@code IdempotentAspect} when a concurrent call with the same
 * idempotency key is already being processed: {@code tryAcquire} could not
 * obtain the lease and no cached response exists yet for that key.
 *
 * <p>The {@code jdempotent} module does not depend on {@code web}, so it
 * cannot translate this into an HTTP status itself. Integration modules are
 * expected to catch this exception and map it to {@code 409 Conflict} with
 * an {@code IN_PROGRESS} error code (e.g. a {@code @ExceptionHandler} in the
 * consuming application, or a future {@code web} module handler).</p>
 */
@Getter
public class IdempotentInProgressException extends RuntimeException {

    private final IdempotencyKey key;

    public IdempotentInProgressException(IdempotencyKey key) {
        super("Idempotent request already in progress for key: " + (key != null ? key.getKeyValue() : null));
        this.key = key;
    }
}
