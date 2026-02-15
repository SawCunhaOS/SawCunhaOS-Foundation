
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

package br.com.sawcunhaos.foundation.utils.enums;

import br.com.sawcunhaos.foundation.utils.specification.ExceptionCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum ScosExceptionCode implements ExceptionCode {
    ATTRIBUTE_NOT_VALID("SCOS-001"),
    ENUM_ERROR("SCOS-002"),
    GENERIC("SCOS-003"),
    ACCESS_DENIED("SCOS-004"),
    TOKEN_NOT_PROVIDED("SCOS-005"),

    TAX_IDENTIFIER_INVALID("SCOS-006"),
    CPF_INVALID("SCOS-007"),
    CNPJ_INVALID("SCOS-008"),

    ;

    private final String code;
}
