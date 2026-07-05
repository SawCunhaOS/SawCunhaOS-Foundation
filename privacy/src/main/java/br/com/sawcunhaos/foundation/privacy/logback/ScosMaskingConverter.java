
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
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

/**
 * Logback composite converter that masks a rendered sub-pattern — register it as {@code %mask(%msg)}.
 *
 * <p>As a {@link CompositeConverter} it wraps a child pattern (typically {@code %msg}) and applies
 * {@link MaskingEngine#maskText(String)} to its rendered text, so PII written in free text
 * (e.g. {@code log.info("cpf {}", cpf)}) is masked even when it does not go through the HTTP filter, while
 * the rest of the line (timestamp, level) is left untouched. The engine is shared statically because
 * Logback instantiates converters reflectively, outside the Spring context;
 * {@link ScosMaskingConverterSupport} wires the singleton engine at startup.</p>
 */
public class ScosMaskingConverter extends CompositeConverter<ILoggingEvent> {

    @Override
    protected String transform(final ILoggingEvent event, final String in) {
        final MaskingEngine engine = ScosMaskingConverterSupport.engine();
        if (engine == null || in == null) {
            return in;
        }
        return engine.maskText(in);
    }
}
