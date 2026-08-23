
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

package br.com.sawcunhaos.foundation.exception.support;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal controller fixture for {@code @WebMvcTest} in this module — exists only
 * to give {@code DispatcherServlet} a real, mapped route so
 * {@code HttpRequestMethodNotSupportedException} (wrong HTTP method) and
 * {@code HttpMediaTypeNotAcceptableException} (unsupported {@code Accept}) can be
 * exercised through actual MVC dispatch, alongside the unmapped-route case.
 */
@RestController
public class PingController {

	@GetMapping(value = "/ping", produces = "application/json")
	public String ping() {
		return "pong";
	}
}
