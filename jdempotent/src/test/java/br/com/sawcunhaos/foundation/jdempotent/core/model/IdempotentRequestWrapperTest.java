
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

package br.com.sawcunhaos.foundation.jdempotent.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Story 3.15 (AC #2): {@code equals()} used to compare {@code obj} against each element of
 * the wrapped {@code request} list instead of comparing two wrappers, breaking both
 * reflexivity and symmetry.
 */
class IdempotentRequestWrapperTest {

    @Test
    void given_a_wrapper_when_compared_to_itself_then_equals_is_reflexive() {
        IdempotentRequestWrapper wrapper = new IdempotentRequestWrapper("payload");

        assertTrue(wrapper.equals(wrapper));
    }

    @Test
    void given_two_wrappers_with_the_same_request_when_compared_then_equals_is_symmetric_and_true() {
        IdempotentRequestWrapper a = new IdempotentRequestWrapper(List.of("payload"));
        IdempotentRequestWrapper b = new IdempotentRequestWrapper(List.of("payload"));

        assertTrue(a.equals(b));
        assertEquals(a.equals(b), b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void given_two_wrappers_with_different_requests_when_compared_then_equals_is_symmetric_and_false() {
        IdempotentRequestWrapper a = new IdempotentRequestWrapper("payload-a");
        IdempotentRequestWrapper b = new IdempotentRequestWrapper("payload-b");

        assertFalse(a.equals(b));
        assertEquals(a.equals(b), b.equals(a));
    }

    @Test
    void given_a_wrapper_when_compared_to_an_element_of_its_own_request_list_then_equals_is_false() {
        // Pins down the old bug: equals() used to return true here because it checked
        // whether "payload" (an element of the request list) equaled itself, instead of
        // comparing two IdempotentRequestWrapper instances.
        IdempotentRequestWrapper wrapper = new IdempotentRequestWrapper("payload");

        assertFalse(wrapper.equals("payload"));
    }

    @Test
    void given_a_wrapper_when_compared_to_null_or_another_type_then_equals_is_false() {
        IdempotentRequestWrapper wrapper = new IdempotentRequestWrapper("payload");

        assertFalse(wrapper.equals(null));
        assertFalse(wrapper.equals("not a wrapper"));
    }
}
