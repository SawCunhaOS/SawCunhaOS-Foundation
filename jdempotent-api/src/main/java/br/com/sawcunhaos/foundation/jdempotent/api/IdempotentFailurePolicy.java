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
 * Declares what happens to the idempotency key when a method annotated with
 * {@link JdempotentResource} throws a business exception.
 */
public enum IdempotentFailurePolicy {

    /**
     * Default. The idempotency key is removed when the method throws, so a retry
     * with the same key re-executes the method.
     */
    RELEASE,

    /**
     * The thrown exception is recorded as the cached result instead of removing
     * the key, so a retry with the same key replays the same failure instead of
     * re-executing the method (avoids duplicating a side effect that already ran
     * before the exception was thrown).
     */
    KEEP_FAILED
}
