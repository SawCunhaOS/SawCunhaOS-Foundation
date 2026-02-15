
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

package br.com.sawcunhaos.foundation.exception.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public record ExceptionResponse(
        String message,
        String codeError,
        List<AttributeNotValid> validationErrors
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 5688741746859700020L;

}
