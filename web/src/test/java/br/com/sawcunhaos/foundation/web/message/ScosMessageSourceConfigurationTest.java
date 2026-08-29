
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

package br.com.sawcunhaos.foundation.web.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the classpath scan in {@link ScosMessageSourceConfiguration#messageSource()}
 * dedupes each bundle family (no locale-suffixed file leaking in as its own basename)
 * and that this bean wins the {@code messageSource} name over Spring Boot's own
 * {@link MessageSourceAutoConfiguration}, which is what {@code before =} on the class
 * annotation is for.
 */
class ScosMessageSourceConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MessageSourceAutoConfiguration.class,
                    ScosMessageSourceConfiguration.class));

    @Test
    @DisplayName("registra um único MessageSource, vencendo o auto-configurado pelo Spring Boot")
    void registersSingleMessageSourceWinningOverSpringBootDefault() {
        contextRunner.run((AssertableApplicationContext context) ->
                assertThat(context).hasSingleBean(MessageSource.class));
    }

    @Test
    @DisplayName("agrega bundles de scos_message/*.properties num único basename por família (sem duplicar por locale)")
    void dedupesBasenamesByBundleFamily() {
        contextRunner.run((AssertableApplicationContext context) -> {
            var messageSource = (ReloadableResourceBundleMessageSource) context.getBean(MessageSource.class);
            assertThat(messageSource.getBasenameSet())
                    .containsExactly("classpath:scos_message/scos_utils_messages");
        });
    }

    @Test
    @DisplayName("resolve o mesmo código em textos diferentes por locale")
    void resolvesLocaleSpecificText() {
        contextRunner.run((AssertableApplicationContext context) -> {
            var messageSource = context.getBean(MessageSource.class);
            assertThat(messageSource.getMessage("SCOS-001", null, Locale.of("en")))
                    .isEqualTo("The attributes informed do not match what was expected. Please check the method documentation.");
            assertThat(messageSource.getMessage("SCOS-001", null, Locale.of("pt", "BR")))
                    .isEqualTo("Os atributos informados não correspondem ao esperado. Por favor, verifique a documentação do método.");
        });
    }

    /**
     * Regression test for the actual multi-module scenario a consumer like SCOS-Flow hits:
     * two separate jars on the classpath, each shipping its own {@code scos_message/} bundle.
     * {@code classpath:} (singular) resolves the scan's root directory via
     * {@link ClassLoader#getResource(String)}, which returns only the FIRST matching classpath
     * root - the second module's bundle would be silently invisible. {@code classpath*:} resolves
     * it via {@link ClassLoader#getResources(String)} (plural), which aggregates every matching
     * root. This builds two real classpath roots via a throwaway {@link URLClassLoader} to prove
     * both bundle families are discovered, not just one.
     */
    @Test
    @DisplayName("descobre bundles de módulos diferentes no classpath (não só o primeiro encontrado)")
    void discoversBundlesFromMultipleClasspathRoots(@TempDir Path moduleA, @TempDir Path moduleB) throws Exception {
        Files.createDirectories(moduleA.resolve("scos_message"));
        Files.writeString(moduleA.resolve("scos_message/module_a_messages.properties"), "code=from-module-a");
        Files.createDirectories(moduleB.resolve("scos_message"));
        Files.writeString(moduleB.resolve("scos_message/module_b_messages.properties"), "code=from-module-b");

        URL[] roots = {moduleA.toUri().toURL(), moduleB.toUri().toURL()};
        Thread currentThread = Thread.currentThread();
        ClassLoader original = currentThread.getContextClassLoader();
        try (URLClassLoader multiModuleClassLoader = new URLClassLoader(roots, original)) {
            currentThread.setContextClassLoader(multiModuleClassLoader);

            var messageSource = (ReloadableResourceBundleMessageSource)
                    new ScosMessageSourceConfiguration().messageSource();

            assertThat(messageSource.getBasenameSet())
                    .contains("classpath:scos_message/module_a_messages", "classpath:scos_message/module_b_messages");
        } finally {
            currentThread.setContextClassLoader(original);
        }
    }
}
