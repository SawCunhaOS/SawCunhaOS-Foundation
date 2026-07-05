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

package br.com.sawcunhaos.foundation.utils.annotation.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * On method-level: declares the audit action to record.
     * On type-level: ignored (Hibernate listener handles C/U/D automatically).
     */
    AuditAction action() default AuditAction.INSERT;

    /**
     * On entity class-level: when {@code true}, Hibernate PostLoad events for this entity
     * will also emit {@code ActionType.SELECT} audit entries. Default {@code false} — opt-in.
     * Has no effect on method-level usage.
     */
    boolean auditRead() default false;

    /**
     * Entity name override for method-level audit. If empty, resolved via return type.
     */
    String entity() default "";

    /**
     * SpEL expression to extract idEntity from method parameters (e.g., "#id.toString()").
     * If empty, resolved via @Id field of return type.
     */
    String idEntitySpEL() default "";

}
