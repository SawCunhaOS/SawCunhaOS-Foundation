
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

package br.com.sawcunhaos.foundation.privacy.specification;

import br.com.sawcunhaos.foundation.privacy.model.DataMask;

import java.util.Set;

/**
 * Optional SPI for declaring masking rules programmatically.
 *
 * <p>The <strong>primary</strong> source of rules is the YAML file (see {@code privacy-masking.yml}). This
 * interface is an <strong>optional override</strong>: an application implements it only when it needs dynamic
 * rules in code that should be added on top of the YAML. The engine flattens sources in the precedence
 * order <em>builtins &rarr; YAML &rarr; SPI</em>, so values returned here win by key over the YAML.</p>
 *
 * <p>All methods default to an empty set, so implementing a subset is fine.</p>
 */
public interface DataMaskingValues {

    /**
     * @return key-based rules applied to HTTP headers
     */
    default Set<DataMask> headersValue() {
        return Set.of();
    }

    /**
     * @return key-based rules applied to JSON body fields
     */
    default Set<DataMask> bodyValue() {
        return Set.of();
    }

    /**
     * @return text-based rules (literal or regex) applied to free-text log messages
     */
    default Set<DataMask> logPatterns() {
        return Set.of();
    }

    /**
     * @return the set of field names whose values must be encrypted at rest in the audit trail
     */
    default Set<String> auditEncryptFields() {
        return Set.of();
    }
}
