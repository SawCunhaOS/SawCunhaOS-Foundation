
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

package br.com.sawcunhaos.foundation.utils.lgpd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SanitizationHeadersComponentTest {

    @Mock
    private DataMaskingService dataMaskingService;

    @InjectMocks
    private SanitizationHeadersComponent sanitizationHeadersComponent;

    private static final String KEY = "KEY";
    private static final String VALUE = "Value";
    private static final String VALUE_MASKED = "*********";

    @Test
    void shouldReturnHeaderValueWithoutApplyingMask() {
        HttpHeaders headers = createHttpHeaders();

        when(dataMaskingService.applyDataMaskValueHeader(KEY, VALUE)).thenReturn(VALUE);

        String result = sanitizationHeadersComponent.sanitizeHeader(headers);
        assertEquals("Header Name -> KEY -- Value\n", result);
    }

    @Test
    void shouldReturnHeaderValueWithMask() {
        HttpHeaders headers = createHttpHeaders();

        when(dataMaskingService.applyDataMaskValueHeader(KEY, VALUE)).thenReturn(VALUE_MASKED);

        String result = sanitizationHeadersComponent.sanitizeHeader(headers);
        assertEquals("Header Name -> KEY -- *********\n", result);
    }

    private HttpHeaders createHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(KEY, VALUE);

        return headers;
    }

}
