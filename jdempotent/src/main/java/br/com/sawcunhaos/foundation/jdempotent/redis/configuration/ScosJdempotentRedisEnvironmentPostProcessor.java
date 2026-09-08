
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

package br.com.sawcunhaos.foundation.jdempotent.redis.configuration;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Story 3.14: this class used to be declared in
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}, a SPI
 * read only during {@code ApplicationContext} refresh — but {@link EnvironmentPostProcessor} is a
 * different, earlier SPI, read by {@link SpringApplication} before the {@code ApplicationContext}
 * even exists. Declaring it in the wrong file meant {@link #postProcessEnvironment} was never
 * invoked.
 *
 * <p>Now registered in {@code META-INF/spring.factories} (key
 * {@code org.springframework.boot.EnvironmentPostProcessor}), not the
 * {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports} convention
 * the story's Dev Notes describe: verified empirically (a self-checking test failed against that
 * file) and by decompiling this project's actual pinned Spring Boot dependency (4.1.x) —
 * {@code EnvironmentPostProcessorsFactory.fromSpringFactories()} builds a
 * {@code SpringFactoriesEnvironmentPostProcessorsFactory} backed by
 * {@code SpringFactoriesLoader.forDefaultResourceLocation()}, which only reads
 * {@code META-INF/spring.factories} — it has no {@code .imports}-file support at all. The
 * {@code .env.EnvironmentPostProcessor} interface (and its {@code .imports} convention) is a
 * separate, deprecated compatibility path this version still loads in addition to the current
 * {@code org.springframework.boot.EnvironmentPostProcessor} used here.</p>
 *
 * <p>{@code @AutoConfiguration}/{@code @ConditionalOnProperty} were removed: this class no longer
 * runs as a {@code @Configuration} bean during context refresh, and an {@link EnvironmentPostProcessor}
 * runs before any Spring container exists, so it cannot rely on bean-conditional evaluation. The
 * "enabled" check that {@code @ConditionalOnProperty} used to express is now a direct read of the
 * property from the not-yet-fully-resolved {@link ConfigurableEnvironment}, matching the
 * {@code matchIfMissing = true} semantics of the original annotation.</p>
 */
public class ScosJdempotentRedisEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final String PROPERTY_SOURCE_NAME = "defaultProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        // String comparison, not getProperty(..., Boolean.class, true): matches the removed
        // @ConditionalOnProperty(havingValue = "true", matchIfMissing = true) semantics exactly —
        // an unparseable value (e.g. "maybe") is simply not "true" here, never a startup-aborting
        // ConversionFailedException, and only the literal "true" enables it, same as the sibling
        // @ConditionalOnProperty-gated beans (no "yes"/"on"/"1" split-brain between this
        // post-processor and them).
        if (!"true".equalsIgnoreCase(environment.getProperty("scos.jdempotent.enabled", "true"))) {
            return;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("spring.data.redis.repositories.enabled", "false");
        addOrReplace(environment.getPropertySources(), map);
    }

    private void addOrReplace(MutablePropertySources propertySources,
                              Map<String, Object> map) {
        MapPropertySource target = null;
        if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
            PropertySource<?> source = propertySources.get(PROPERTY_SOURCE_NAME);
            if (source instanceof MapPropertySource) {
                target = (MapPropertySource) source;
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    if (!target.containsProperty(entry.getKey())) {
                        target.getSource().put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        if (target == null) {
            target = new MapPropertySource(PROPERTY_SOURCE_NAME, map);
        }
        if (!propertySources.contains(PROPERTY_SOURCE_NAME)) {
            propertySources.addLast(target);
        }
    }
}
