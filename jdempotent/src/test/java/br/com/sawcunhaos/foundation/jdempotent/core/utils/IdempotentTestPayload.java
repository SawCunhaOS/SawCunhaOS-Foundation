
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

package br.com.sawcunhaos.foundation.jdempotent.core.utils;

import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentId;
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentIgnore;
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentProperty;
import lombok.Data;

@Data
public class IdempotentTestPayload {
    private String name;
    @JdempotentIgnore
    private Long age;

    @JdempotentProperty("transactionId")
    private Long eventId;

    // Story 3.12: exists only to prove @JdempotentId never composes the key — no existing
    // test asserts on it, and the new JdempotentIdAnnotationChain link excludes it before it
    // ever reaches IdempotentIgnorableWrapper#nonIgnoredFields.
    @JdempotentId
    private String generatedId;

    public IdempotentTestPayload() {
    }

    public IdempotentTestPayload(String name) {
        this.name = name;
    }
}
