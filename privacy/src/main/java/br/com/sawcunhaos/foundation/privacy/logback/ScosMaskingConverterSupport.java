
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

package br.com.sawcunhaos.foundation.privacy.logback;

import br.com.sawcunhaos.foundation.privacy.core.MaskingEngine;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

/**
 * Wires the singleton {@link MaskingEngine} into Logback and registers the {@code %mask} conversion rule
 * <strong>programmatically</strong>, so the application does not have to edit its {@code logback.xml}.
 *
 * <p>Logback creates converters by reflection, outside the Spring context, so the engine is held in a
 * static reference set once at startup by the auto-configuration. To opt out of the programmatic
 * registration, set {@code scos.privacy.log.register-converter=false} (handled by the auto-config) or
 * include {@code logback-privacy.xml} manually via {@code <include>}.</p>
 */
public final class ScosMaskingConverterSupport {

    /** Conversion word registered in the LoggerContext, usable as {@code %mask(%msg)}. */
    public static final String CONVERSION_WORD = "mask";

    /** Conversion word for masking a single MDC value, usable as {@code %maskmdc{theKey}}. */
    public static final String MDC_CONVERSION_WORD = "maskmdc";

    private static volatile MaskingEngine engine;

    private ScosMaskingConverterSupport() {
    }

    /**
     * Registers the engine and the {@code %mask} conversion rule on the current LoggerContext.
     *
     * <p>Logback's {@code PatternLayout} merges its default converters with the per-context registry kept
     * under {@link ch.qos.logback.core.CoreConstants#PATTERN_RULE_REGISTRY}; adding our word there makes
     * {@code %mask} usable without touching {@code logback.xml}.</p>
     *
     * @param maskingEngine the singleton engine (must not be {@code null})
     */
    @SuppressWarnings("unchecked")
    public static void register(final MaskingEngine maskingEngine) {
        engine = maskingEngine;
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext context)) {
            return;
        }
        final String registryKey = ch.qos.logback.core.CoreConstants.PATTERN_RULE_REGISTRY;
        java.util.Map<String, String> registry =
            (java.util.Map<String, String>) context.getObject(registryKey);
        if (registry == null) {
            registry = new java.util.HashMap<>();
            context.putObject(registryKey, registry);
        }
        registry.put(CONVERSION_WORD, ScosMaskingConverter.class.getName());
        registry.put(MDC_CONVERSION_WORD, ScosMaskingMdcConverter.class.getName());
    }

    /** @return the wired engine, or {@code null} when not yet registered */
    public static MaskingEngine engine() {
        return engine;
    }
}
