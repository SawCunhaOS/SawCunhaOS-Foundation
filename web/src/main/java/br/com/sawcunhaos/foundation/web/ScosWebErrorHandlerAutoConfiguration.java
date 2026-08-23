
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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Auto-configuration that registers {@link ExceptionsHandler} as a bean, so it loads
 * simply by being on the classpath (imported via {@code AutoConfiguration.imports}),
 * without requiring {@code @ComponentScan} in the consuming application.
 *
 * <p>{@link ExceptionsHandler} itself stays a plain {@code @ControllerAdvice} (its
 * internal logic is unchanged by this move); this class only wraps it into an explicit
 * {@code @Bean} method so Spring can apply {@link ConditionalOnProperty} and
 * {@link Order} to it.</p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "scos.web.error-handler", name = "enabled", matchIfMissing = true)
public class ScosWebErrorHandlerAutoConfiguration {

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	public ExceptionsHandler exceptionsHandler(LocaleService localeService) {
		return new ExceptionsHandler(localeService);
	}

}
