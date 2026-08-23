
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

package br.com.sawcunhaos.foundation.audit.listener.sample;

import br.com.sawcunhaos.foundation.audit.api.Auditable;

/**
 * Fixture for {@code AuditableAnnotationCountListenerTest}: 1 type-level {@code @Auditable}
 * usage (this class itself) + 1 method-level usage.
 */
@Auditable
public class SampleAuditableUsage {

    @Auditable
    public void auditedMethod() {
    }

    public void notAudited() {
    }

}
