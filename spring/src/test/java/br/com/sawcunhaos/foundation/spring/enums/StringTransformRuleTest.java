
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

package br.com.sawcunhaos.foundation.spring.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StringTransformRuleTest {

    @Test
    void camelCaseLowercasesValue() {
        assertEquals("hello world", StringTransformRule.CAMEL_CASE.apply("HELLO WORLD"));
    }

    @Test
    void camelCaseHandlesNullAndEmpty() {
        assertNull(StringTransformRule.CAMEL_CASE.apply(null));
        assertEquals("", StringTransformRule.CAMEL_CASE.apply(""));
    }

    @Test
    void upperCaseUppercasesValue() {
        assertEquals("HELLO", StringTransformRule.UPPER_CASE.apply("hello"));
    }

    @Test
    void lowerCaseLowercasesValue() {
        assertEquals("hello", StringTransformRule.LOWER_CASE.apply("HELLO"));
    }

    @Test
    void capitalizeCapitalizesEachWord() {
        assertEquals("Hello World", StringTransformRule.CAPITALIZE.apply("hello WORLD"));
    }
}
