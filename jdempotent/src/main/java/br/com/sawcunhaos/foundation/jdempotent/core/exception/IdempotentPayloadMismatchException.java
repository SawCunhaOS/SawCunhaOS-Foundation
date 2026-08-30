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
 * Thrown by {@code IdempotentAspect} when a call arrives with the same idempotency
 * key as another call (in progress, or already finished and cached) but a different
 * payload hash (Story 3.6): a collision under the same key, not a genuine retry of
 * the same call.
 *
 * <p>Takes precedence over both outcomes that {@code IdempotentInProgressException}
 * and a replayed cached response would otherwise produce, including in the race
 * window where the first call is still in progress — the payload mismatch is the
 * more specific error and must win.</p>
 *
 * <p>The {@code jdempotent} module does not depend on {@code web}, so it cannot
 * translate this into an HTTP status itself. Integration modules are expected to
 * catch this exception and map it to {@code 422 Unprocessable Entity} with a
 * {@code PAYLOAD_MISMATCH} error code (e.g. a {@code @ExceptionHandler} in the
 * consuming application, or a future {@code web} module handler) — same pattern
 * used by {@link IdempotentInProgressException} (Story 3.5).</p>
 */
@Getter
public class IdempotentPayloadMismatchException extends RuntimeException {

    private final IdempotencyKey key;

    public IdempotentPayloadMismatchException(IdempotencyKey key) {
        super("Idempotent request payload mismatch for key: " + (key != null ? key.getKeyValue() : null));
        this.key = key;
    }
}
