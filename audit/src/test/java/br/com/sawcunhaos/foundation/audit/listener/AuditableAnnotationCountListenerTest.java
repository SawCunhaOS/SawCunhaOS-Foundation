
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

package br.com.sawcunhaos.foundation.audit.listener;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditableAnnotationCountListenerTest {

    @Test
    void countsTypeLevelAndMethodLevelAuditableUsage() {
        int count = new AuditableAnnotationCountListener()
                .countAnnotatedElements(List.of("br.com.sawcunhaos.foundation.audit.listener.sample"));

        assertEquals(2, count);
    }

    @Test
    void returnsZeroForAPackageWithNoUsages() {
        int count = new AuditableAnnotationCountListener()
                .countAnnotatedElements(List.of("br.com.sawcunhaos.foundation.audit.listener.empty"));

        assertEquals(0, count);
    }

}
