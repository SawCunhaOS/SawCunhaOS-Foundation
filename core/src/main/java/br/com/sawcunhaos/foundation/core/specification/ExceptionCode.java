
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

package br.com.sawcunhaos.foundation.core.specification;

import java.net.URI;

/**
 * Contract for error codes used across the foundation.
 *
 * <p>Beyond the stable string {@link #getCode()}, this interface carries the
 * metadata required by RFC 9457 (Problem Details for HTTP APIs): a {@code type}
 * URI and a human-readable {@code title}. Both are provided as {@code default}
 * methods so existing implementors keep compiling without changes.</p>
 */
public interface ExceptionCode {

    /**
     * Base URI for the foundation problem-type documentation. The concrete
     * pages do not need to exist yet; they are stable identifiers per RFC 9457.
     */
    String PROBLEM_TYPE_BASE_URI = "https://docs.sawcunhaos.com.br/problems/";

    /**
     * Stable error code (e.g. {@code "SCOS-001"}).
     */
    String getCode();

    /**
     * RFC 9457 {@code type} URI identifying the problem category.
     *
     * <p>Derivation rule: the code is lower-cased and underscores become
     * hyphens, then appended to {@link #PROBLEM_TYPE_BASE_URI}. Example:
     * {@code SCOS-001} → {@code https://docs.sawcunhaos.com.br/problems/scos-001}.</p>
     *
     * <p>Override only when a code must point to a curated, non-derivable URI.</p>
     */
    default URI getType() {
        return URI.create(PROBLEM_TYPE_BASE_URI + getCode().toLowerCase().replace("_", "-"));
    }

    /**
     * RFC 9457 {@code title}: a short, fixed, human-readable summary of the
     * problem category. Defaults to {@code "Error"}.
     *
     * <p>Implementors SHOULD override this to provide a category-specific title
     * (e.g. {@code "Validation Error"}, {@code "Access Denied"}).</p>
     */
    default String getTitle() {
        return "Error";
    }

    default int getHttpCode() {
        return 400;
    }
}
