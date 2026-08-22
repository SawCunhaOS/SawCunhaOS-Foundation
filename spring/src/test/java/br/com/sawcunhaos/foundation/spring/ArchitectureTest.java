
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

package br.com.sawcunhaos.foundation.spring;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Local ArchUnit rule (AD-5, AC #2) that keeps {@code scos-foundation-spring} free of
 * Servlet/JPA/Spring Data: it depends on {@code core} plus {@code spring-context}/{@code spring-aop}
 * only, so consumers of {@code ScosRule} don't have to inherit JPA/Servlet.
 */
@AnalyzeClasses(packages = "br.com.sawcunhaos.foundation.spring",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule noServletNoJpaNoSpringData = noClasses()
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.servlet..", "jakarta.persistence..", "org.springframework.data..");

}
