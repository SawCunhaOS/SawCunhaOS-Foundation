
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

package br.com.sawcunhaos.foundation.jdempotent.core.chain;

import br.com.sawcunhaos.foundation.jdempotent.core.model.ChainData;
import br.com.sawcunhaos.foundation.jdempotent.core.model.KeyValuePair;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.IdempotentTestPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class JdempotentIdAnnotationChainTest {

    @InjectMocks
    private JdempotentIdAnnotationChain jdempotentIdAnnotationChain;

    @Mock
    private JdempotentPropertyAnnotationChain jdempotentPropertyAnnotationChain;

    @Test
    void should_process_and_return_empty_pair_for_jdempotent_id_field() throws IllegalAccessException, NoSuchFieldException {
        //Given
        jdempotentIdAnnotationChain.next(jdempotentPropertyAnnotationChain);
        IdempotentTestPayload idempotentTestPayload = new IdempotentTestPayload();
        idempotentTestPayload.setGeneratedId("some-generated-key");
        ChainData chainData = new ChainData();
        chainData.setArgs(idempotentTestPayload);
        chainData.setDeclaredField(idempotentTestPayload.getClass().getDeclaredField("generatedId"));

        //When
        KeyValuePair process = jdempotentIdAnnotationChain.process(chainData);

        //Then
        assertNull(process.getKey());
        assertNull(process.getValue());
        verifyNoInteractions(jdempotentPropertyAnnotationChain);
    }

    @Test
    void should_delegate_to_next_chain_when_field_has_no_jdempotent_id() throws IllegalAccessException, NoSuchFieldException {
        //Given
        jdempotentIdAnnotationChain.next(jdempotentPropertyAnnotationChain);
        IdempotentTestPayload idempotentTestPayload = new IdempotentTestPayload();
        idempotentTestPayload.setName("name");
        ChainData chainData = new ChainData();
        chainData.setArgs(idempotentTestPayload);
        chainData.setDeclaredField(idempotentTestPayload.getClass().getDeclaredField("name"));

        //When
        jdempotentIdAnnotationChain.process(chainData);

        //Then
        verify(jdempotentPropertyAnnotationChain).process(eq(chainData));
    }
}
