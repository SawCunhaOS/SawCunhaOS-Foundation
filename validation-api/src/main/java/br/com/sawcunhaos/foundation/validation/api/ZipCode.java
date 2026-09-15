
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
 * ({@code ZipCodeValidator}) lives in {@code scos-foundation-validation}, which depends on this
 * module — referencing it back here would create a module cycle. The binding is restored at
 * runtime via the XML constraint mapping shipped in {@code scos-foundation-validation}
 * ({@code META-INF/validation.xml}).
 */
@Target( { ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {})
public @interface ZipCode {

    /**
     * Error code carried on the violation when the annotated value is not a valid CEP — not a
     * literal message. It is a {@code SCOS-XXX} code (see {@code ScosExceptionCode}, in
     * {@code scos-foundation-core}), resolved to human text outside this module. Overriding this
     * with a literal message bypasses that resolution — the violation surfaces the literal text
     * instead of the translated code. This default currently reuses {@code SCOS-009}, which
     * {@code ScosExceptionCode} also assigns to {@code EMAIL_INVALID} — the registry has no
     * dedicated CEP code yet.
     */
    String message() default "SCOS-009";

    /**
     * Jakarta Bean Validation group(s) this constraint belongs to. Not used by
     * {@code ZipCodeValidator} — present only to satisfy the {@code @Constraint} contract.
     */
    Class<?>[] groups() default {};

    /**
     * Client-supplied metadata attached to a violation. Not used by {@code ZipCodeValidator} —
     * present only to satisfy the {@code @Constraint} contract.
     */
    Class<? extends Payload>[] payload() default {};

}
