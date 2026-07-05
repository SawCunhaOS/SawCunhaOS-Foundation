
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

package br.com.sawcunhaos.foundation.privacy;

/**
 * Sanitizes a JSON request/response body by applying the key-based masking rules.
 *
 * <p>The tree is parsed once and walked in place (see {@code JsonMasker}), so cost is O(n) over the
 * payload rather than the previous re-parse per nesting level. Stateless and thread-safe.</p>
 */
public class SanitizationBodyComponent {

    private final DataMaskingService dataMaskingService;

    public SanitizationBodyComponent(final DataMaskingService dataMaskingService) {
        this.dataMaskingService = dataMaskingService;
    }

    /**
     * Masks a JSON body. Non-JSON input is returned unchanged.
     *
     * @param body the JSON document
     * @return the masked JSON
     */
    public String sanitize(final String body) {
        return dataMaskingService.applyDataMaskBody(body);
    }
}
