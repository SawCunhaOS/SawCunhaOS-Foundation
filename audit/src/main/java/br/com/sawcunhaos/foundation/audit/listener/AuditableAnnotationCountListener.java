
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

import br.com.sawcunhaos.foundation.audit.api.Auditable;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Story 1.15 (AC #1): logs, once at startup, how many {@code @Auditable} usages (type-level on
 * entities, method-level on service methods) were found on the consuming application's own
 * classpath — the visible signal that {@code audit-api}'s annotation actually has this
 * implementation module wired in, not just compiled against.
 */
@Component
@ConditionalOnProperty(prefix = "scos.audit", name = "enabled", havingValue = "true")
@Log4j2
public class AuditableAnnotationCountListener implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<String> basePackages;
        try {
            basePackages = AutoConfigurationPackages.get(event.getApplicationContext().getBeanFactory());
        } catch (IllegalStateException e) {
            // No @EnableAutoConfiguration base package registered (a non-standard bootstrap, e.g. no
            // @SpringBootApplication) — this is an observability nicety, never worth crashing startup over.
            log.warn("AuditableAnnotationCountListener: could not resolve the application's base package(s), skipping the scan", e);
            return;
        }
        int count = countAnnotatedElements(basePackages);
        log.info("AuditableAnnotationCountListener: found {} @Auditable usage(s) on the classpath", count);
    }

    /**
     * Package-visible for direct testing without booting a full {@link ApplicationReadyEvent}.
     */
    int countAnnotatedElements(List<String> basePackages) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        TypeFilter matchAny = (metadataReader, metadataReaderFactory) -> true;
        scanner.addIncludeFilter(matchAny);

        // A Set, not a running total per basePackage: AutoConfigurationPackages.get() can return
        // overlapping/nested packages, which would otherwise double-count the same class.
        Set<String> scannedClassNames = new LinkedHashSet<>();
        for (String basePackage : basePackages) {
            scanner.findCandidateComponents(basePackage)
                    .forEach(candidate -> scannedClassNames.add(candidate.getBeanClassName()));
        }

        int count = 0;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (String className : scannedClassNames) {
            try {
                // initialize=false: reflect on the class without running its static initializers.
                Class<?> clazz = Class.forName(className, false, classLoader);
                if (clazz.isAnnotationPresent(Auditable.class)) count++;
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Auditable.class)) count++;
                }
            } catch (ClassNotFoundException | LinkageError e) {
                log.warn("AuditableAnnotationCountListener: skipped unloadable class {}", className, e);
            }
        }
        return count;
    }

}
