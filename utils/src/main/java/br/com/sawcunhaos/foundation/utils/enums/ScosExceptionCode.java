
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

/**
 * Foundation-provided error codes.
 *
 * <p>Each constant carries an RFC 9457 {@code title} via {@code title}, exposed
 * through the Lombok-generated {@code getTitle()} which overrides the
 * {@link ExceptionCode#getTitle()} default. The {@code type} URI is derived from
 * the code by {@link ExceptionCode#getType()} and is not overridden here.</p>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum ScosExceptionCode implements ExceptionCode {

    /** Bean/field validation failure (HTTP 400). */
    ATTRIBUTE_NOT_VALID("SCOS-001", "Validation Error"),
    /** Invalid enum value while deserializing the request body (HTTP 400). */
    ENUM_ERROR("SCOS-002", "Validation Error"),
    /** Generic, uncategorized error fallback (HTTP 500). */
    GENERIC("SCOS-003", "Internal Server Error"),
    /** Authenticated principal lacks permission for the resource (HTTP 403). */
    ACCESS_DENIED("SCOS-004", "Access Denied"),
    /** No authentication token supplied (HTTP 401). */
    TOKEN_NOT_PROVIDED("SCOS-005", "Unauthorized"),

    /** Tax identifier (CPF/CNPJ) is structurally invalid (HTTP 400). */
    TAX_IDENTIFIER_INVALID("SCOS-006", "Validation Error"),
    /** CPF is invalid (HTTP 400). */
    CPF_INVALID("SCOS-007", "Validation Error"),
    /** CNPJ is invalid (HTTP 400). */
    CNPJ_INVALID("SCOS-008", "Validation Error"),
    /** E-mail is invalid (HTTP 400). */
    EMAIL_INVALID("SCOS-009", "Validation Error"),

    ;

    private final String code;
    private final String title;
}
