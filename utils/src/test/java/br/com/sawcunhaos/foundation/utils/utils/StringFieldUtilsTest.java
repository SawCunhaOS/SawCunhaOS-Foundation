package br.com.sawcunhaos.foundation.utils.utils;

import br.com.sawcunhaos.foundation.utils.utils.dto.TestDTO;
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
