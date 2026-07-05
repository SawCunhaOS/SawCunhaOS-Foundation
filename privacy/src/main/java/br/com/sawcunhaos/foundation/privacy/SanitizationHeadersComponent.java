
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sanitizes HTTP headers by applying the key-based masking rules to each header value.
 *
 * <p>Stateless and thread-safe: delegates to the immutable {@link DataMaskingService}.</p>
 */
public class SanitizationHeadersComponent {

    private final DataMaskingService dataMaskingService;

    public SanitizationHeadersComponent(final DataMaskingService dataMaskingService) {
        this.dataMaskingService = dataMaskingService;
    }

    /**
     * Returns a copy of the headers with sensitive values masked.
     *
     * @param headers the original header map (not mutated)
     * @return a new map with masked values
     */
    public Map<String, String> sanitize(final Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return headers;
        }
        final Map<String, String> result = new LinkedHashMap<>(headers.size());
        headers.forEach((k, v) -> result.put(k, dataMaskingService.applyDataMaskValueHeader(k, v)));
        return result;
    }
}
