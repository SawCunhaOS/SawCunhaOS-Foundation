
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

package br.com.sawcunhaos.foundation.privacy.spring;

import br.com.sawcunhaos.foundation.privacy.DataMaskingService;
import br.com.sawcunhaos.foundation.privacy.core.MaskingEngine;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the module loads purely from the classpath via auto-configuration (no {@code @ComponentScan})
 * and that every bean recedes when the application defines its own.
 */
class ScosPrivacyAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ScosPrivacyAutoConfiguration.class));

    @Test
    void beansLoadFromClasspathOnly() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(MaskingEngine.class);
            assertThat(context).hasSingleBean(DataMaskingService.class);
        });
    }

    @Test
    void disabledByPropertySkipsBeans() {
        runner.withPropertyValues("scos.privacy.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(MaskingEngine.class));
    }

    @Test
    void applicationBeanOverridesLibraryBean() {
        runner.withUserConfiguration(CustomEngineConfig.class)
            .run(context -> {
                assertThat(context).hasSingleBean(MaskingEngine.class);
                assertThat(context.getBean(MaskingEngine.class))
                    .isSameAs(CustomEngineConfig.CUSTOM);
            });
    }

    @Configuration
    static class CustomEngineConfig {
        static final MaskingEngine CUSTOM = MaskingEngine.builder().build();

        @Bean
        MaskingEngine maskingEngine() {
            return CUSTOM;
        }
    }
}
