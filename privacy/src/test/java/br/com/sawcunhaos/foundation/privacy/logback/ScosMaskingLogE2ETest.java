
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
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check that a log line rendered through the programmatically-registered {@code %mask} conversion
 * word comes out masked. Uses a {@link ListAppender} to capture the event, then formats it with a
 * {@link PatternLayout} bound to the same {@link LoggerContext} where {@link ScosMaskingConverterSupport}
 * registered the rule.
 */
class ScosMaskingLogE2ETest {

    @Test
    void maskWordMasksRenderedMessage() throws Exception {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        try (InputStream yaml = getClass().getResourceAsStream("/test-masking-basic.yml")) {
            ScosMaskingConverterSupport.register(MaskingEngine.fromYaml(yaml));
        }

        final Logger logger = context.getLogger("privacy-log-e2e");
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        logger.addAppender(appender);

        logger.info("user said topsecret out loud");

        final PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern("%mask(%msg)");
        layout.start();

        assertThat(appender.list).hasSize(1);
        final String rendered = layout.doLayout(appender.list.get(0));

        assertThat(rendered).doesNotContain("topsecret").contains("***");
    }
}
