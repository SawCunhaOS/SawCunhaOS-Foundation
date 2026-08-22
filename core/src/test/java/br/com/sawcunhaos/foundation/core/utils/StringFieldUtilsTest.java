
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

package br.com.sawcunhaos.foundation.core.utils;

import br.com.sawcunhaos.foundation.core.utils.dto.TestDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StringFieldUtilsTest {

    @Test
    void testStringsUpperCase() {
        TestDTO testDTO = createTestDTO();

        StringFieldUtils.applyUpperCase(testDTO);

        assertEquals("DESCRIPTION", testDTO.getDescription());
        assertNull(testDTO.getTaxIdentifier());
        assertEquals("NAME TEST DTO", testDTO.getName());
    }

    @Test
    void testStringsLowerCase() {
        TestDTO testDTO = createTestDTO();

        StringFieldUtils.applyLowerCase(testDTO);

        assertEquals("description", testDTO.getDescription());
        assertNull(testDTO.getTaxIdentifier());
        assertEquals("name test dto", testDTO.getName());
    }

    @Test
    void testStringsCamelCase() {
        TestDTO testDTO = createTestDTO();

        StringFieldUtils.applyCamelCase(testDTO);

        assertEquals("description", testDTO.getDescription());
        assertNull(testDTO.getTaxIdentifier());
        assertEquals("name test dto", testDTO.getName());
    }

    @Test
    void testStringsCapitalize() {
        TestDTO testDTO = createTestDTO();

        StringFieldUtils.applyCapitalize(testDTO);

        assertEquals("Description", testDTO.getDescription());
        assertNull(testDTO.getTaxIdentifier());
        assertEquals("Name Test Dto", testDTO.getName());
    }

    private TestDTO createTestDTO() {
        return TestDTO.builder()
                .age(1)
                .id(1L)
                .description("DescriPtioN")
                .name("Name Test DTO")
                .build();
    }
}
