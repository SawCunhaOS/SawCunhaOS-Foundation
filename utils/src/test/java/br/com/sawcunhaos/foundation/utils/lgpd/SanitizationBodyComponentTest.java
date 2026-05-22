
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

import br.com.sawcunhaos.foundation.utils.dto.response.ScosResponseDTO;
import br.com.sawcunhaos.foundation.utils.utils.GsonUtils;
import br.com.sawcunhaos.foundation.utils.utils.dto.TestDTO;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.netty.util.internal.StringUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class SanitizationBodyComponentTest {

    @Mock
    private DataMaskingService dataMaskingService;

    @InjectMocks
    private SanitizationBodyComponent sanitizationBodyComponent;

    private final Gson gson = GsonUtils.getInstance();
    private static final String TAX_IDENTIFIER = "111.222.333-44";
    private static final String BODY_IS_EMPTY = "Does not have Body";

    @Test
    void shouldReturnTheSameBodyWhenEnteringStringThatIsNotJson() {
        String body = "Test.body";

        String bodyResponse = sanitizationBodyComponent.sanitizeBody(body);

        assertEquals(body, bodyResponse);
    }

    @Test
    void shouldReturnMessageOfNotHavingBodyWhenEnteringEmptyString() {
        String bodyResponse = sanitizationBodyComponent.sanitizeBody(StringUtil.EMPTY_STRING);

        assertEquals(BODY_IS_EMPTY, bodyResponse);
    }

    @Test
    void shouldSanitizeTheBodyWithoutAlteringAnyOriginalRecords() {
        TestDTO testDTO = createTestDTO(TAX_IDENTIFIER, false);
        String body = gson.toJson(createInsideSoftwaresResponse(testDTO));

        Mockito.when(dataMaskingService.applyDataMaskValueBody(any(), any(JsonElement.class))).thenReturn(gson.toJsonTree(null));
        Mockito.when(dataMaskingService.applyDataMaskValueBody("taxIdentifier", gson.toJsonTree(TAX_IDENTIFIER))).thenReturn(gson.toJsonTree(TAX_IDENTIFIER));
        Mockito.when(dataMaskingService.applyDataMaskValueBody("enable", gson.toJsonTree(false))).thenReturn(gson.toJsonTree(false));
        Mockito.when(dataMaskingService.applyDataMaskValueBody("age", gson.toJsonTree(0))).thenReturn(gson.toJsonTree(0));

        String bodyResponse = sanitizationBodyComponent.sanitizeBody(body);

        assertEquals(body, bodyResponse);
    }

    @Test
    void shouldSanitizeTheBodyByChangingTheTaxIdentifierRecord() {
        TestDTO testDTO = createTestDTO(TAX_IDENTIFIER, false);
        String body = gson.toJson(createInsideSoftwaresResponse(testDTO));
        TestDTO testDTOExpected = createTestDTO("*******", false);
        String bodyExpected = gson.toJson(createInsideSoftwaresResponse(testDTOExpected));

        Mockito.when(dataMaskingService.applyDataMaskValueBody(any(), any(JsonElement.class))).thenReturn(gson.toJsonTree(null));
        Mockito.when(dataMaskingService.applyDataMaskValueBody("taxIdentifier", gson.toJsonTree(TAX_IDENTIFIER))).thenReturn(gson.toJsonTree("*******"));
        Mockito.when(dataMaskingService.applyDataMaskValueBody("enable", gson.toJsonTree(false))).thenReturn(gson.toJsonTree(false));
        Mockito.when(dataMaskingService.applyDataMaskValueBody("age", gson.toJsonTree(0))).thenReturn(gson.toJsonTree(0));

        String bodyResponse = sanitizationBodyComponent.sanitizeBody(body);

        assertEquals(bodyExpected, bodyResponse);
    }

    @Test
    void shouldSanitizeTheBodyByChangingTheTaxIdentifierRecordOfBodyAndList() {
        TestDTO testDTO = createTestDTO(TAX_IDENTIFIER, true);
        String body = gson.toJson(createInsideSoftwaresResponse(testDTO));
        TestDTO testDTOExpected = createTestDTO("*******", true);
        String bodyExpected = gson.toJson(createInsideSoftwaresResponse(testDTOExpected));

        Mockito.when(dataMaskingService.applyDataMaskValueBody(any(), any(JsonElement.class))).thenReturn(gson.toJsonTree(null));
        Mockito.when(dataMaskingService.applyDataMaskValueBody("taxIdentifier", gson.toJsonTree(TAX_IDENTIFIER))).thenReturn(gson.toJsonTree("*******"));
        Mockito.when(dataMaskingService.applyDataMaskValueBody("enable", gson.toJsonTree(false))).thenReturn(gson.toJsonTree(false));
        Mockito.when(dataMaskingService.applyDataMaskValueBody("age", gson.toJsonTree(0))).thenReturn(gson.toJsonTree(0));

        String bodyResponse = sanitizationBodyComponent.sanitizeBody(body);

        assertEquals(bodyExpected, bodyResponse);
    }

    @Test
    void shouldSanitizeTheBodyByChangingTheTaxIdentifierRecordOfBodyAndList1() {
        TestDTO testDTO = createTestDTO(TAX_IDENTIFIER, true);
        String body = gson.toJson(createInsideSoftwaresResponse(testDTO));
        InputStream bodyInputStream = new ByteArrayInputStream( body.getBytes() );

        TestDTO testDTOExpected = createTestDTO("*******", true);
        String bodyExpected = gson.toJson(createInsideSoftwaresResponse(testDTOExpected));

        Mockito.when(dataMaskingService.applyDataMaskValueBody(any(), any(JsonElement.class))).thenReturn(gson.toJsonTree(null));
        Mockito.when(dataMaskingService.applyDataMaskValueBody("taxIdentifier", gson.toJsonTree(TAX_IDENTIFIER))).thenReturn(gson.toJsonTree("*******"));
        Mockito.when(dataMaskingService.applyDataMaskValueBody("enable", gson.toJsonTree(false))).thenReturn(gson.toJsonTree(false));
        Mockito.when(dataMaskingService.applyDataMaskValueBody("age", gson.toJsonTree(0))).thenReturn(gson.toJsonTree(0));

        String bodyResponse = sanitizationBodyComponent.sanitizeBody(bodyInputStream);

        assertEquals(bodyExpected, bodyResponse);
    }

    private TestDTO createTestDTO(final String taxIdentifier, final boolean addListTestDTO) {
        Set<TestDTO> testDTOs = null;
        if(addListTestDTO) {
            testDTOs = new HashSet<>();
            testDTOs.add(
                    TestDTO.builder().taxIdentifier(taxIdentifier).build()
            );
        }

        return TestDTO.builder()
                .taxIdentifier(taxIdentifier)
                .testDTOs(testDTOs)
                .build();
    }

    private ScosResponseDTO createInsideSoftwaresResponse(final TestDTO testDTO) {

        return ScosResponseDTO.builder()
                .data(testDTO)
                .build();
    }
}
