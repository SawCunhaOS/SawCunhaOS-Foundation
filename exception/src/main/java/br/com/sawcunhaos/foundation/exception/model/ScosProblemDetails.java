
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

import br.com.sawcunhaos.foundation.utils.specification.ExceptionCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Central factory for RFC 9457 {@link ProblemDetail} responses across the
 * foundation (web {@code @ControllerAdvice}, the security access-denied handler
 * and the security exception filter all funnel through here).
 *
 * <p>Design choice: we build {@code ProblemDetail} via {@link ProblemDetail#forStatus}
 * and attach SCOS-specific fields through {@code setProperty(...)} instead of
 * subclassing {@code ProblemDetail}. Subclasses with their own getters would
 * duplicate fields in the JSON (once via the parent {@code properties} map and
 * once via the child getter).</p>
 *
 * <p>SCOS extension fields added on top of the standard
 * {@code type/title/status/detail/instance}: {@code code}, optional
 * {@code errors}, optional {@code requestId} (from MDC) and {@code timestamp}.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScosProblemDetails {

    /** MDC key holding the per-request correlation id. */
    public static final String MDC_REQUEST_ID = "X-Request-ID";

    /**
     * Builds a problem from an {@link ExceptionCode}, using its {@code type} and
     * {@code title} metadata. Use this when the originating enum is available
     * (e.g. validation and security handlers).
     */
    public static ProblemDetail of(HttpStatusCode status, ExceptionCode code, String detail, String instance) {
        ProblemDetail problem = base(status, code.getType(), code.getTitle(), detail, instance);
        problem.setProperty("code", code.getCode());
        return problem;
    }

    /**
     * Builds a validation problem from an {@link ExceptionCode} plus the
     * field-level {@code errors} array.
     */
    public static ProblemDetail ofValidation(HttpStatusCode status, ExceptionCode code, String detail,
                                             String instance, List<ScosFieldError> errors) {
        ProblemDetail problem = base(status, code.getType(), code.getTitle(), detail, instance);
        problem.setProperty("code", code.getCode());
        problem.setProperty("errors", errors);
        return problem;
    }

    /**
     * Builds a problem when only the string {@code code} survives (e.g.
     * {@code ScosException} stores the code as a {@code String}). The {@code type}
     * URI is derived from the code with the same rule as
     * {@link ExceptionCode#getType()}.
     */
    public static ProblemDetail of(HttpStatusCode status, String code, String title, String detail, String instance) {
        ProblemDetail problem = base(status, typeFromCode(code), title, detail, instance);
        problem.setProperty("code", code);
        return problem;
    }

    /**
     * Derives the RFC 9457 {@code type} URI from a string code: lower-case,
     * underscores to hyphens, appended to {@link ExceptionCode#PROBLEM_TYPE_BASE_URI}.
     */
    public static URI typeFromCode(String code) {
        return URI.create(ExceptionCode.PROBLEM_TYPE_BASE_URI + code.toLowerCase().replace("_", "-"));
    }

    /**
     * Centralizes correlation and traceability enrichment: copies the MDC
     * {@code X-Request-ID} into {@code requestId} when present (silently skipped
     * otherwise) and always stamps an ISO-8601 UTC {@code timestamp}.
     */
    public static ProblemDetail enrich(ProblemDetail problem) {
        String requestId = MDC.get(MDC_REQUEST_ID);
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    private static ProblemDetail base(HttpStatusCode status, URI type, String title, String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(type);
        problem.setTitle(title);
        problem.setDetail(detail);
        if (instance != null) {
            problem.setInstance(URI.create(instance));
        }
        return problem;
    }
}
