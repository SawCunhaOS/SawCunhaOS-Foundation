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

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Local ArchUnit rules (AD-5) that keep {@code scos-foundation-validation-api} from turning into a
 * new {@code utils}: only {@code @interface}/{@code enum}, no runtime dependency beyond the JDK and
 * {@code jakarta.validation-api} (the single dependency this module is allowed, per AC #1 of Story
 * 1.5 and the frozen Story 1.1 inventory).
 */
@AnalyzeClasses(packages = "br.com.sawcunhaos.foundation.validation.api",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule onlyAnnotationsAndEnums = classes()
            .should(new ArchCondition<JavaClass>("be an annotation type or an enum") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    boolean satisfied = item.isAnnotation() || item.isEnum();
                    events.add(new SimpleConditionEvent(item, satisfied,
                            item.getFullName() + " is neither an annotation type nor an enum"));
                }
            });

    @ArchTest
    static final ArchRule noRuntimeDependencyBeyondJdkAndJakartaValidation = classes()
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("java..", "jakarta.validation..",
                    "br.com.sawcunhaos.foundation.validation.api..");

}
