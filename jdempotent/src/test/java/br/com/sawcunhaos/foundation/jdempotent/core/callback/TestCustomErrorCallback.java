
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

package br.com.sawcunhaos.foundation.jdempotent.core.callback;

import br.com.sawcunhaos.foundation.jdempotent.core.utils.IdempotentTestPayload;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.TestException;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class TestCustomErrorCallback implements ErrorConditionalCallback {
    @Override
    public boolean onErrorCondition(Object response) {
        if(ObjectUtils.isEmpty(((IdempotentTestPayload) response).getName())){
            return false;
        }
        return ((IdempotentTestPayload) response).getName().equalsIgnoreCase("test");
    }

    @Override
    public RuntimeException onErrorCustomException() {
        return new TestException("Name will not be test");
    }
}
