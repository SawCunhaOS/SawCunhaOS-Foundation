
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

package br.com.sawcunhaos.foundation.archtest;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Cross-module rules (AD-5, Story 1.14) — the only kind that belongs in {@code archtest}. Rules
 * that cite a single module (e.g. "{@code core} imports no Spring") are born inside that module
 * instead, in the same commit that creates it (Stories 1.7-1.13).
 *
 * <p>{@code @AnalyzeClasses(packages = "br.com.sawcunhaos.foundation")} scans the aggregated test
 * classpath, i.e. every module {@code archtest} declares as a test dependency in its {@code pom.xml}
 * — a module absent from that list is invisible to these rules, not exempt from them.</p>
 */
@AnalyzeClasses(packages = "br.com.sawcunhaos.foundation",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /**
     * AC #1: nothing in this reactor may depend on {@code web} except a real consuming
     * application — and no consuming application is part of this build, so the checkable form of
     * the rule is simply "no reactor module (other than {@code web} itself) imports {@code web}".
     */
    @ArchTest
    static final ArchRule nothingDependsOnWeb = noClasses()
            .that().resideOutsideOfPackage("br.com.sawcunhaos.foundation.web..")
            .should().dependOnClassesThat()
            .resideInAPackage("br.com.sawcunhaos.foundation.web..");

    /**
     * AC #2: no cycle between any two modules of the reactor. Slices are the first package segment
     * under {@code br.com.sawcunhaos.foundation} (one per Maven module, e.g. {@code core},
     * {@code cache}, {@code audit}) — {@code *-api} modules share their parent module's first
     * segment (e.g. {@code audit}/{@code audit-api} both slice under "audit"), which is harmless
     * here: the {@code *-api} modules are dependency-free leaves (Story 1.5), so they cannot
     * participate in a cycle regardless of which slice they're counted in.
     */
    @ArchTest
    static final ArchRule noCyclesBetweenModules = slices()
            .matching("br.com.sawcunhaos.foundation.(*)..")
            .should().beFreeOfCycles();

}
