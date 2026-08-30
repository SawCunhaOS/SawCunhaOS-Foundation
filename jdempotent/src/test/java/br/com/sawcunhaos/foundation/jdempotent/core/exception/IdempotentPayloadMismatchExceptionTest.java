
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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotentPayloadMismatchExceptionTest {

    @Test
    void given_a_key_when_constructed_then_message_contains_the_key_value_and_getKey_returns_it() {
        IdempotencyKey key = new IdempotencyKey("123");

        IdempotentPayloadMismatchException exception = new IdempotentPayloadMismatchException(key);

        assertSame(key, exception.getKey());
        assertTrue(exception.getMessage().contains("123"));
    }

    @Test
    void given_a_null_key_when_constructed_then_no_npe_and_message_reflects_null() {
        IdempotentPayloadMismatchException exception = assertDoesNotThrow(
                () -> new IdempotentPayloadMismatchException(null)
        );

        assertNull(exception.getKey());
        assertTrue(exception.getMessage().contains("null"));
    }
}
