
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
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Logback converter that masks a single MDC value by key — register it as {@code %maskmdc{theKey}}.
 *
 * <p>Where {@link ScosMaskingConverter} masks the free-text message ({@code maskText}), this converter
 * targets <em>structured</em> context: it reads {@code MDC[key]} and applies
 * {@link MaskingEngine#maskStructured(String, String)} so a known field placed in the MDC (e.g.
 * {@code MDC.put("cpf", cpf)}, then {@code %maskmdc{cpf}} in the pattern) is masked by the same key-based
 * rules that protect HTTP bodies. The engine is shared statically because Logback instantiates converters
 * outside the Spring context (see {@link ScosMaskingConverterSupport}).</p>
 */
public class ScosMaskingMdcConverter extends ClassicConverter {

    private String key;

    @Override
    public void start() {
        this.key = getFirstOption();
        super.start();
    }

    @Override
    public String convert(final ILoggingEvent event) {
        if (key == null) {
            return "";
        }
        final String value = event.getMDCPropertyMap().get(key);
        final MaskingEngine engine = ScosMaskingConverterSupport.engine();
        if (engine == null || value == null) {
            return value == null ? "" : value;
        }
        return engine.maskStructured(key, value);
    }
}
