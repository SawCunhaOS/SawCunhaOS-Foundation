
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

import br.com.sawcunhaos.foundation.utils.annotation.jdempotent.JdempotentIgnore;
import br.com.sawcunhaos.foundation.utils.annotation.jdempotent.JdempotentProperty;
import lombok.Data;

@Data
public class IdempotentTestPayload {
    private String name;
    @JdempotentIgnore
    private Long age;

    @JdempotentProperty("transactionId")
    private Long eventId;

    public IdempotentTestPayload() {
    }

    public IdempotentTestPayload(String name) {
        this.name = name;
    }
}
