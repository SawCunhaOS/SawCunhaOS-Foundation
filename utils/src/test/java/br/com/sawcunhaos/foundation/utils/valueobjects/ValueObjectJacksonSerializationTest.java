
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

package br.com.sawcunhaos.foundation.utils.valueobjects;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Story 1.3 / AC6 inventory: none of these value objects had a custom Gson {@code TypeAdapter} — they
 * were never passed to Gson directly in production code (callers stringify them first). Jackson uses
 * their Lombok-generated getters (unlike Gson's private-field reflection), so this confirms they don't
 * regress to an empty {@code {}} now that Jackson is the only serializer in the codebase.
 */
class ValueObjectJacksonSerializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void cpfSerializesFieldsNotEmptyObject() {
        String json = MAPPER.writeValueAsString(new Cpf("11144477735"));
        assertNotEquals("{}", json);
        assertEquals("{\"cpf\":\"11144477735\",\"type\":\"CPF\"}", json);
    }

    @Test
    void cnpjSerializesFieldsNotEmptyObject() {
        String json = MAPPER.writeValueAsString(new Cnpj("11222333000181"));
        assertNotEquals("{}", json);
        assertEquals("{\"cnpj\":\"11222333000181\",\"type\":\"CNPJ\"}", json);
    }

    @Test
    void emailSerializesFieldNotEmptyObject() {
        String json = MAPPER.writeValueAsString(new Email("user@example.com"));
        assertNotEquals("{}", json);
        assertEquals("{\"email\":\"user@example.com\"}", json);
    }

    @Test
    void taxIdentifierSerializesFieldsNotEmptyObject() {
        String json = MAPPER.writeValueAsString(new TaxIdentifier("11144477735"));
        assertNotEquals("{}", json);
        assertEquals("{\"taxIdentifier\":\"11144477735\",\"type\":\"CPF\"}", json);
    }
}
