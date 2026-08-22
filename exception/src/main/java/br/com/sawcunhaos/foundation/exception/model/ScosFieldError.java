
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

import br.com.sawcunhaos.foundation.core.specification.ExceptionCode;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;

/**
 * Single field-level validation error inside an RFC 9457 problem response
 * (carried in the {@code errors} extension array).
 *
 * <p>Named {@code ScosFieldError} (not {@code FieldError}) on purpose: it must
 * coexist with {@link org.springframework.validation.FieldError} in the same
 * classes without an import clash.</p>
 *
 * @param pointer JSON Pointer (RFC 6901) to the offending field, e.g.
 *                {@code "#/email"} for {@code email} or {@code "#/address/street"}
 *                for the nested {@code address.street}.
 * @param detail  localized validation message for the field.
 */
public record ScosFieldError(
        String pointer,
        String detail,
        String code,
        String type
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 7702315274859700021L;

    /**
     * Builds a {@code ScosFieldError} converting a dotted field path into a JSON
     * Pointer: prefixes {@code "#/"} and replaces {@code "."} with {@code "/"}.
     *
     * @param field   field path as reported by validation (e.g. {@code "email"},
     *                {@code "address.street"}).
     * @param detail  localized validation message.
     * @param code   stable error code (e.g. {@code "VALIDATION_ERROR"}).
     * @param type   error type (e.g. {@code "https://tools.ietf.org/html/rfc7807"}).
     */
    public static ScosFieldError of(String field, String detail, String code) {
        return new ScosFieldError("#/" + field.replace(".", "/"), detail, code, typeFromCode(code).toString());
    }

    /**
     * Derives the RFC 9457 {@code type} URI from a string code: lower-case,
     * underscores to hyphens, appended to {@link ExceptionCode#PROBLEM_TYPE_BASE_URI}.
     */
    private static URI typeFromCode(String code) {
        return URI.create(ExceptionCode.PROBLEM_TYPE_BASE_URI + code.toLowerCase().replace("_", "-"));
    }
}
