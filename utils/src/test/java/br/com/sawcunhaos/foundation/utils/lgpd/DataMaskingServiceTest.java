
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

import br.com.sawcunhaos.foundation.utils.lgpd.model.DataMask;
import br.com.sawcunhaos.foundation.utils.lgpd.specification.DataMaskingValues;
import br.com.sawcunhaos.foundation.utils.utils.GsonUtils;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataMaskingServiceTest {

    @Mock
    private DataMaskingValues dataMaskingValues;

    @InjectMocks
    private DataMaskingService dataMaskingService;

    private static final String KEY = "KEY";
    private static final String VALUE_MASKED = "*********";

    @Test
    void shouldApplyMaskNoValueSuccessfullyUsingMetodoApplyDataMaskValueHeader() {
        String value = "VALOR_MASCARA";
        DataMask dataMask = createDataMask(KEY, VALUE_MASKED, false, null);

        when(dataMaskingValues.headersValue()).thenReturn(Set.of(dataMask));

        String valueResult = dataMaskingService.applyDataMaskValueHeader(KEY, value);

        assertEquals(VALUE_MASKED, valueResult);
    }

    @Test
    void shouldntApplyTheMaskWhenYouHaveNotConfiguredDataMaskUsingApplyDataMaskValueHeaderMethod() {
        String value = "VALOR_MASCARA";

        when(dataMaskingValues.headersValue()).thenReturn(Set.of());

        String valueResult = dataMaskingService.applyDataMaskValueHeader(KEY, value);

        assertEquals(value, valueResult);
    }

    @Test
    void shouldntApplyTheMaskWhenNotFindTheConfigurationDataMaskUsingMetodoApplyDataMaskValueHeader() {
        String value = "VALOR_MASCARA";
        DataMask dataMask = createDataMask(KEY, VALUE_MASKED, false, null);

        when(dataMaskingValues.headersValue()).thenReturn(Set.of(dataMask));

        String valueResult = dataMaskingService.applyDataMaskValueHeader("OTHER_KEY", value);

        assertEquals(value, valueResult);
    }

    @Test
    void shouldApplyMaskNoValueSuccessfullyUsingMetodoApplyDataMaskValueBody() {
        String value = "VALOR_MASCARA";
        DataMask dataMask = createDataMask(KEY, VALUE_MASKED, false, null);

        when(dataMaskingValues.bodyValue()).thenReturn(Set.of(dataMask));

        String valueResult = dataMaskingService.applyDataMaskValueBody(KEY, value);

        assertEquals(VALUE_MASKED, valueResult);
    }

    @Test
    void shouldntApplyTheMaskWhenYouHaveNotConfiguredDataMaskUsingApplyDataMaskValueBodyMethod() {
        String value = "VALOR_MASCARA";

        when(dataMaskingValues.bodyValue()).thenReturn(Set.of());

        String valueResult = dataMaskingService.applyDataMaskValueBody(KEY, value);

        assertEquals(value, valueResult);
    }

    @Test
    void shouldntApplyTheMaskWhenNotFindTheConfigurationDataMaskUsingMetodoApplyDataMaskValueBody() {
        String value = "VALOR_MASCARA";
        DataMask dataMask = createDataMask(KEY, VALUE_MASKED, false, null);

        when(dataMaskingValues.bodyValue()).thenReturn(Set.of(dataMask));

        String valueResult = dataMaskingService.applyDataMaskValueBody("OTHER_KEY", value);

        assertEquals(value, valueResult);
    }


    @Test
    void shouldApplyMaskWithRegexNoValorWithSuccessUsingMetodoApplyDataMaskValueHeader() {
        String value = "VALOR_MASCARA";
        String newValue = "*";
        String valueExpected = "V*L***M*SC***";
        DataMask dataMask = createDataMask(KEY, newValue, true, "([AOR_])");

        when(dataMaskingValues.headersValue()).thenReturn(Set.of(dataMask));

        String valueResult = dataMaskingService.applyDataMaskValueHeader(KEY, value);

        assertEquals(valueExpected, valueResult);
    }

    @Test
    void shouldApplyMaskWithRegexNoValorWithSuccessUsingMetodoApplyDataMaskValueBody() {
        String value = "VALOR_MASCARA";
        String newValue = "*";
        String valueExpected = "V*L***M*SC***";
        DataMask dataMask = createDataMask(KEY, newValue, true, "([AOR_])");

        when(dataMaskingValues.bodyValue()).thenReturn(Set.of(dataMask));

        String valueResult = dataMaskingService.applyDataMaskValueBody(KEY, value);

        assertEquals(valueExpected, valueResult);
    }

    @Test
    void shouldApplyMaskWithRegexNoValorWithSucessUsingMetodoApplyDataMaskValueBodyByJsonElement() {
        String value = "VALOR_MASCARA";
        String newValue = "*";
        String valueExpected = "\"V*L***M*SC***\"";
        DataMask dataMask = createDataMask(KEY, newValue, true, "([AOR_])");

        when(dataMaskingValues.bodyValue()).thenReturn(Set.of(dataMask));

        JsonElement valueResult = dataMaskingService.applyDataMaskValueBody(KEY, GsonUtils.getInstance().toJsonTree(value));

        assertEquals(valueExpected, valueResult.getAsString());
    }

    private DataMask createDataMask(
            final String key,
            final String newValue,
            final boolean isRegex,
            final String regex
    ) {
        return DataMask.builder()
                .key(key)
                .newValue(newValue)
                .isRegex(isRegex)
                .regex(regex)
                .build();

    }
}
