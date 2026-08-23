
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

package br.com.sawcunhaos.foundation.jdempotent.core.listener;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdempotentAnnotationCountListenerTest {

    @Test
    void countsJdempotentResourceMethodsAndJdempotentIdFields() {
        int count = new JdempotentAnnotationCountListener()
                .countAnnotatedElements(List.of("br.com.sawcunhaos.foundation.jdempotent.core.listener.sample"));

        assertThat(count).isEqualTo(3);
    }

    @Test
    void returnsZeroForAPackageWithNoUsages() {
        int count = new JdempotentAnnotationCountListener()
                .countAnnotatedElements(List.of("br.com.sawcunhaos.foundation.jdempotent.core.listener.empty"));

        assertThat(count).isZero();
    }

}
