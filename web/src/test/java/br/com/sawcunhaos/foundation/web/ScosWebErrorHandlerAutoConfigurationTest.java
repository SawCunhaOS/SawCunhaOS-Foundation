
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

package br.com.sawcunhaos.foundation.web;

import br.com.sawcunhaos.foundation.core.specification.LocaleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 2.9 (AC #1): proves {@link ExceptionsHandler} is registered through the real
 * auto-configuration mechanism ({@code AutoConfiguration.imports} + {@link ScosWebErrorHandlerAutoConfiguration}),
 * not just by reading the source. Every other {@code ExceptionsHandler*Test} either
 * {@code @InjectMocks} the class directly or {@code @Import}s it manually — none of them
 * would fail if {@code scos.web.error-handler.enabled}'s property name were typo'd or the
 * {@code AutoConfiguration.imports} entry were removed.
 */
class ScosWebErrorHandlerAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(LocaleServiceConfig.class)
			.withConfiguration(AutoConfigurations.of(ScosWebErrorHandlerAutoConfiguration.class));

	@Test
	@DisplayName("registra ExceptionsHandler por padrão (matchIfMissing = true)")
	void registersExceptionsHandlerByDefault() {
		contextRunner.run((AssertableApplicationContext context) ->
				assertThat(context).hasSingleBean(ExceptionsHandler.class));
	}

	@Test
	@DisplayName("não registra ExceptionsHandler quando scos.web.error-handler.enabled=false")
	void doesNotRegisterExceptionsHandlerWhenDisabled() {
		contextRunner.withPropertyValues("scos.web.error-handler.enabled=false")
				.run((AssertableApplicationContext context) ->
						assertThat(context).doesNotHaveBean(ExceptionsHandler.class));
	}

	@Configuration
	static class LocaleServiceConfig {
		@Bean
		LocaleService localeService() {
			return org.mockito.Mockito.mock(LocaleService.class);
		}
	}
}
