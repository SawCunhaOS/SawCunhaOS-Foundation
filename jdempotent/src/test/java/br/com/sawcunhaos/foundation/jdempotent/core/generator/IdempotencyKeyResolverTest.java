
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

package br.com.sawcunhaos.foundation.jdempotent.core.generator;

import br.com.sawcunhaos.foundation.jdempotent.core.aspect.IdempotentAspect;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentIgnorableWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.IdempotentTestPayload;
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentId;
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentProperty;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Story 3.12 (Task 4): {@link IdempotencyKeyResolver} determinism and {@code @JdempotentId}
 * exclusion. Field collection (the {@code annotationChain} walk that produces the canonical,
 * {@code TreeMap}-backed {@code IdempotentIgnorableWrapper}) happens in
 * {@link IdempotentAspect#getIdempotentNonIgnorableWrapper}, reused here directly — no AOP/
 * join point involved — exactly the "resolver usable outside the aspect" shape AC #2 asks for.
 */
class IdempotencyKeyResolverTest {

    private final IdempotentAspect idempotentAspect = new IdempotentAspect();
    private final IdempotencyKeyResolver resolver = new IdempotencyKeyResolver();

    @Test
    void should_produce_the_same_key_regardless_of_field_declaration_order() throws IllegalAccessException {
        //given: two classes with the same @JdempotentProperty keys/values, declared in a
        //different field order.
        FieldsDeclaredAlphaFirst declaredAlphaFirst = new FieldsDeclaredAlphaFirst();
        FieldsDeclaredGammaFirst declaredGammaFirst = new FieldsDeclaredGammaFirst();

        //when
        IdempotencyKey keyOne = resolve(declaredAlphaFirst);
        IdempotencyKey keyTwo = resolve(declaredGammaFirst);

        //then
        assertEquals(keyOne.getKeyValue(), keyTwo.getKeyValue());
    }

    @Test
    void should_not_let_a_jdempotent_id_field_influence_the_resulting_key() throws IllegalAccessException {
        //given: same @JdempotentProperty-relevant fields, different @JdempotentId values.
        IdempotentTestPayload withoutGeneratedId = new IdempotentTestPayload("payload");
        withoutGeneratedId.setEventId(1L);

        IdempotentTestPayload withGeneratedId = new IdempotentTestPayload("payload");
        withGeneratedId.setEventId(1L);
        withGeneratedId.setGeneratedId("some-previously-generated-key");

        //when
        IdempotencyKey keyWithout = resolve(withoutGeneratedId);
        IdempotencyKey keyWith = resolve(withGeneratedId);

        //then
        assertEquals(keyWithout.getKeyValue(), keyWith.getKeyValue());
    }

    @Test
    void should_exclude_a_field_that_carries_both_jdempotent_id_and_jdempotent_property() throws IllegalAccessException {
        //given: a field annotated with both @JdempotentId and @JdempotentProperty at once —
        //fillChains() checks @JdempotentId before @JdempotentProperty (Story 3.12, AC #1), so
        //@JdempotentId must win and the field must never reach nonIgnoredFields.
        FieldWithBothAnnotations payload = new FieldWithBothAnnotations();

        //when
        IdempotentIgnorableWrapper wrapper = idempotentAspect.getIdempotentNonIgnorableWrapper(List.of(payload));

        //then
        assertFalse(wrapper.getNonIgnoredFields().containsKey("dual"));
    }

    private IdempotencyKey resolve(Object payload) throws IllegalAccessException {
        IdempotentRequestWrapper requestWrapper =
                new IdempotentRequestWrapper(idempotentAspect.getIdempotentNonIgnorableWrapper(List.of(payload)));
        return resolver.resolve(requestWrapper, "listener");
    }

    @Data
    private static class FieldWithBothAnnotations {
        @JdempotentId
        @JdempotentProperty("dual")
        private String field = "value";
    }

    @Data
    private static class FieldsDeclaredAlphaFirst {
        @JdempotentProperty("alpha")
        private String first = "a-value";
        @JdempotentProperty("beta")
        private String second = "b-value";
        @JdempotentProperty("gamma")
        private String third = "c-value";
    }

    @Data
    private static class FieldsDeclaredGammaFirst {
        @JdempotentProperty("gamma")
        private String third = "c-value";
        @JdempotentProperty("beta")
        private String second = "b-value";
        @JdempotentProperty("alpha")
        private String first = "a-value";
    }
}
