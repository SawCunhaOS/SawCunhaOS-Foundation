
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

package br.com.sawcunhaos.foundation.validation.api;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code validatedBy} is intentionally empty: the {@code ConstraintValidator} implementation
 * ({@code CpfValidator}) lives in {@code scos-foundation-validation}, which depends on this
 * module — referencing it back here would create a module cycle. The binding is restored at
 * runtime via the XML constraint mapping shipped in {@code scos-foundation-validation}
 * ({@code META-INF/validation.xml}).
 */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CPF {

    /**
     * Error code carried on the violation when the annotated value is not a valid CPF — not a
     * literal message. It is a {@code SCOS-XXX} code (see {@code ScosExceptionCode}, in
     * {@code scos-foundation-core}), resolved to human text outside this module. Overriding this
     * with a literal message (e.g. {@code @CPF(message = "...")}) bypasses that resolution — the
     * violation surfaces the literal text instead of the translated code.
     */
    String message() default "SCOS-007";

    /**
     * Jakarta Bean Validation group(s) this constraint belongs to. Not used by
     * {@code CpfValidator} — present only to satisfy the {@code @Constraint} contract.
     */
    Class<?>[] groups() default {};

    /**
     * Client-supplied metadata attached to a violation. Not used by {@code CpfValidator} —
     * present only to satisfy the {@code @Constraint} contract.
     */
    Class<? extends Payload>[] payload() default {};
}
